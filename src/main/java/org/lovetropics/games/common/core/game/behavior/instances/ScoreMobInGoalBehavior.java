package org.lovetropics.games.common.core.game.behavior.instances;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.content.MinigameTexts;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.SpawnBuilder;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorTypes;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameLivingEntityEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.core.game.region.Region;
import org.lovetropics.games.common.core.game.region.RegionQuery;
import org.lovetropics.games.common.core.game.state.progress.ProgressChannel;
import org.lovetropics.games.common.core.game.state.progress.ProgressionPeriod;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.game.state.team.GameTeam;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamState;
import org.lovetropics.games.common.util.EntityTemplate;
import org.lovetropics.peekaboo.api.Disguise;
import org.lovetropics.peekaboo.api.EntityDisguiseHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public record ScoreMobInGoalBehavior(
		RegionQuery spawnRegion,
		EntityTemplate scoringEntity,
		Optional<StatisticKey<Integer>> targetCountStatistic,
		Map<GameTeamKey, RegionQuery> teamGoalRegions,
		StatisticKey<Integer> statistic,
		int spawnDelay,
		// TODO: Split the scoring, and just detect entities dying?
		ProgressChannel channel,
		Optional<ProgressionPeriod> scorePeriod
) implements IGameBehavior {
	public static final MapCodec<ScoreMobInGoalBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegionQuery.CODEC.fieldOf("spawn_region").forGetter(ScoreMobInGoalBehavior::spawnRegion),
			EntityTemplate.CODEC.fieldOf("scoring_entity").forGetter(ScoreMobInGoalBehavior::scoringEntity),
			StatisticKey.INT_CODEC.optionalFieldOf("target_count_statistic").forGetter(ScoreMobInGoalBehavior::targetCountStatistic),
			Codec.unboundedMap(GameTeamKey.CODEC, RegionQuery.CODEC).fieldOf("team_goal_regions").forGetter(ScoreMobInGoalBehavior::teamGoalRegions),
			StatisticKey.typedCodec(Integer.class).fieldOf("statistic").forGetter(ScoreMobInGoalBehavior::statistic),
			Codec.INT.optionalFieldOf("spawn_delay", SharedConstants.TICKS_PER_SECOND * 2).forGetter(ScoreMobInGoalBehavior::spawnDelay),
			ProgressChannel.CODEC.optionalFieldOf("channel", ProgressChannel.MAIN).forGetter(ScoreMobInGoalBehavior::channel),
			ProgressionPeriod.CODEC.optionalFieldOf("score_period").forGetter(ScoreMobInGoalBehavior::scorePeriod)
	).apply(i, ScoreMobInGoalBehavior::new));

	private static final Component POSITIVE_EMOTE = Component.literal("⭐").withStyle(ChatFormatting.GOLD);
	private static final Component NEGATIVE_EMOTE = Component.literal("☠").withStyle(ChatFormatting.RED);

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.SCORE_MOB_IN_GOAL;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		Region spawnRegion = this.spawnRegion.one(game);

		TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);
		if (teams.getTeamKeys().size() != 2) {
			throw new GameException(Component.literal("Only 2 teams are supported"));
		}

		List<Goal> goals = new ArrayList<>();
		for (Map.Entry<GameTeamKey, RegionQuery> entry : teamGoalRegions.entrySet()) {
			GameTeamKey defensiveTeam = entry.getKey();
			GameTeamKey offensiveTeam = teams.getTeamKeys().stream().filter(k -> !k.equals(defensiveTeam)).findFirst().orElseThrow();
			goals.add(new Goal(entry.getValue(), defensiveTeam, offensiveTeam));
		}

		MutableInt activeCount = new MutableInt();
		MutableInt spawnDelay = new MutableInt(0);

		events.listen(GamePhaseEvents.TICK, () -> {
			int targetCount = resolveTargetCount(game);
			if (activeCount.getValue() >= targetCount) {
				return;
			}
			if (spawnDelay.getValue() > 0) {
				spawnDelay.decrement();
				if (spawnDelay.getValue() % SharedConstants.TICKS_PER_SECOND == 0) {
					game.allPlayers().playSound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.25f, 1.0f);
				}
			} else {
				spawnScoringEntity(spawnRegion);
				activeCount.increment();
				spawnDelay.setValue(this.spawnDelay);
			}
		});

		BooleanSupplier canScore = scorePeriod.map(period -> period.createPredicate(game, channel)).orElse(() -> true);

		events.listen(GameLivingEntityEvents.TICK, (level, entity) -> {
			if (!canScoreWith(entity) || !canScore.getAsBoolean()) {
				return;
			}
			Goal goal = getGoalContaining(game, goals, entity);
			if (goal != null) {
				if (!scoreGoal(game, teams, goal, entity)) {
					return;
				}
				if (entity instanceof ServerPlayer player) {
					SpawnBuilder spawn = new SpawnBuilder(player);
					game.invoker(GamePlayerEvents.SPAWN).onSpawn(player.getUUID(), spawn, PlayerRole.PARTICIPANT);
					spawn.teleportAndApply(player);
				} else {
					entity.discard();
					activeCount.decrement();
				}
			}
		});
	}

	private boolean canBeScoredIn(TeamState teams, LivingEntity entity, Goal goal) {
		if (entity instanceof ServerPlayer player) {
			// It's a bit much if the player can
			return !teams.isOnTeam(player, goal.offensiveTeam);
		}
		return true;
	}

	private boolean canScoreWith(LivingEntity entity) {
		if (entity.getType() == scoringEntity.type()) {
			return true;
		}
		if (entity instanceof ServerPlayer player && !player.isSpectator()) {
			Disguise disguise = EntityDisguiseHolder.getDisguise(player);
			return disguise.entity().isPresent() && disguise.entity().get().type() == scoringEntity.type();
		}
		return false;
	}

	private int resolveTargetCount(IGamePhase game) {
		return targetCountStatistic.map(key -> game.statistics().global().getInt(key)).orElse(1);
	}

	private void spawnScoringEntity(Region region) {
		BlockPos spawnPos = findSuitableSpawn(region);
		Entity entity = scoringEntity.spawn(region.level(), spawnPos.getX() + 0.5, spawnPos.getY() + 1.5, spawnPos.getZ() + 0.5);
		region.level().broadcastEntityEvent(entity, EntityEvent.POOF);
	}

	private BlockPos findSuitableSpawn(Region region) {
		Vec3i size = region.size();
		int halfSizeX = Mth.positiveCeilDiv(size.getX(), 2);
		int halfSizeY = Mth.positiveCeilDiv(size.getY(), 2);
		int halfSizeZ = Mth.positiveCeilDiv(size.getZ(), 2);
		BlockPos bestPos = region.centerBlock();
		int bestEntityCount = Integer.MAX_VALUE;
		for (BlockPos pos : BlockPos.withinManhattan(region.centerBlock(), halfSizeX, halfSizeY, halfSizeZ)) {
			if (!region.contains(region.level(), pos)) {
				continue;
			}
			int entityCount = region.level().getEntities(scoringEntity.type(), new AABB(pos), e -> true).size();
			if (entityCount == 0) {
				return pos;
			} else if (entityCount < bestEntityCount) {
				bestPos = pos.immutable();
				bestEntityCount = entityCount;
			}
		}
		return bestPos;
	}

	private boolean scoreGoal(IGamePhase game, TeamState teams, Goal goal, LivingEntity entity) {
		ServerPlayer responsiblePlayer = entity.getLastAttacker() instanceof ServerPlayer p ? p : null;
		// PVP crabs!
		if (responsiblePlayer == null && entity instanceof ServerPlayer player) {
			responsiblePlayer = player;
			// Don't let the player walk into the opponent's goal, that's a bit much
			if (teams.isOnTeam(player, goal.offensiveTeam)) {
				return false;
			}
		}

		game.statistics().forTeam(goal.offensiveTeam).incrementInt(statistic, 1);

		// Not necessarily the player who scored
		GameTeam scoringTeam = Objects.requireNonNull(teams.getTeamByKey(goal.offensiveTeam));
		addScoreEffects(game, teams, goal, responsiblePlayer, scoringTeam);

		return true;
	}

	private static void addScoreEffects(IGamePhase game, TeamState teams, Goal goal, @Nullable ServerPlayer scoringPlayer, GameTeam scoringTeam) {
		Component scoringName = scoringPlayer != null ? scoringPlayer.getDisplayName() : MinigameTexts.UNKNOWN;
		boolean opposingGoal = scoringPlayer == null || teams.isOnTeam(scoringPlayer, goal.defensiveTeam);

		Component title = opposingGoal ? NEGATIVE_EMOTE : POSITIVE_EMOTE;
		Component subtitle = MinigameTexts.POINT_SCORED.apply(scoringName, scoringTeam.styledName());

		int fade = SharedConstants.TICKS_PER_SECOND / 4;
		int length = SharedConstants.TICKS_PER_SECOND * 2;
		game.allPlayers().showTitle(title, subtitle, fade, length, fade);
		game.allPlayers().playSound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
	}

	private @Nullable Goal getGoalContaining(IGamePhase game, List<Goal> goals, Entity entity) {
		for (Goal goal : goals) {
			if (goal.region.contains(game, entity)) {
				return goal;
			}
		}
		return null;
	}

	private record Goal(RegionQuery region, GameTeamKey defensiveTeam, GameTeamKey offensiveTeam) {
	}
}
