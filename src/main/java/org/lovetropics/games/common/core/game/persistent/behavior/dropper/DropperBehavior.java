package org.lovetropics.games.common.core.game.persistent.behavior.dropper;

import com.lovetropics.lib.BlockBox;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.persistent.PersistentGame;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehavior;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehaviorType;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehaviors;
import org.lovetropics.games.common.core.game.rewards.GameRewards;
import org.lovetropics.games.common.core.map.MapRegions;
import org.lovetropics.games.common.core.map.SavedRegions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class DropperBehavior implements PersistentGameBehavior {

	private static final String PLAY_STAT_NAME = "Drops";
	private static final String WIN_STAT_NAME = "Wins";
	private static final String FAIL_STAT_NAME = "Fails";

	public static final MapCodec<DropperBehavior> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("win_blocks").forGetter(b -> b.winBlocks),
			Codec.STRING.fieldOf("id").forGetter(b -> b.id),
			Reward.CODEC.optionalFieldOf("first_win_reward", Reward.NONE).forGetter(b -> b.firstWinReward),
			Reward.CODEC.optionalFieldOf("win_reward", Reward.NONE).forGetter(b -> b.winReward)
	).apply(instance, DropperBehavior::new));

	private final String id;
	private final HolderSet<Block> winBlocks;
	private final Reward firstWinReward;
	private final Reward winReward;

	private final List<Vec3> failLocations = new ArrayList<>();
	private final List<Vec3> successLocations = new ArrayList<>();
	private BlockBox dropperRegionBox;
	private Objectives objectives;
	private PlayerTeam team;

	public DropperBehavior(HolderSet<Block> winBlocks, String id, Reward firstWinReward, Reward winReward) {
		this.winBlocks = winBlocks;
		this.id = id;
		this.firstWinReward = firstWinReward;
		this.winReward = winReward;
	}

	@Override
	public void register(PersistentGame game, EventRegistrar events) {
		events.listen(GamePhaseEvents.START, _ -> {
			objectives = Objectives.createObjectives(game.level(), id);
			team = getOrCreateTeam(game.level(), "world_game.dropper." + id);

			String failRegion = this.id + "_fail";
			String successRegion = this.id + "_success";
			String dropperRegion = this.id + "_dropper";
			MapRegions regions = SavedRegions.get(game.level()).regions().compile();
			regions.getAll(failRegion).forEach(blockBox -> failLocations.add(blockBox.center()));
			regions.getAll(successRegion).forEach(blockBox -> successLocations.add(blockBox.center()));
			dropperRegionBox = regions.getAny(dropperRegion);
		});

		events.listen(GamePlayerEvents.ADD, player -> {
			player.level().getScoreboard().addPlayerToTeam(player.getScoreboardName(), team);
			player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, MobEffectInstance.INFINITE_DURATION, 1, true, false, false));
		});
		events.listen(GamePlayerEvents.REMOVE, player -> {
			player.level().getScoreboard().removePlayerFromTeam(player.getScoreboardName(), team);
			player.removeEffect(MobEffects.INVISIBILITY);
		});

		events.listen(GamePhaseEvents.STOP, _ -> {
			for (ServerPlayer player : game.players()) {
				if (dropperRegionBox.contains(player.blockPosition())) {
					Vec3 randomLocation = getRandomLocation(player, failLocations);
					player.teleportTo(randomLocation.x(), randomLocation.y(), randomLocation.z());
				}
			}
		});

		events.listen(GamePhaseEvents.TICK, () -> {
			for (ServerPlayer player : game.players()) {
				if (dropperRegionBox.contains(player.blockPosition())) {
					BlockState blockStateOn = player.getBlockStateOn();
					if (blockStateOn.isAir()) {
						continue;
					}
					this.onPlay(player);
					if (winBlocks.contains(blockStateOn.typeHolder())) {
						this.onWin(player);
					} else {
						this.onFail(player);
					}
				}
			}
		});
	}

	private void onPlay(ServerPlayer player) {
		getScoreAccess(objectives.play(), player).add(1);
		getScoreAccess(objectives.stats(), PLAY_STAT_NAME).add(1);
	}

	private void onWin(ServerPlayer player) {
		int newValue = getScoreAccess(objectives.win(), player).add(1);
		if (newValue == 1) {
			firstWinReward.giveToPlayer(player);
		} else {
			// Only give this away after the first win.
			winReward.giveToPlayer(player);
		}
		getScoreAccess(objectives.stats(), WIN_STAT_NAME).add(1);
		Vec3 randomLocation = getRandomLocation(player, successLocations);
		player.level().playSeededSound(null, randomLocation.x(), randomLocation.y(), randomLocation.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.VOICE, 5f, 1.0F, player.getRandom().nextLong());
		player.teleportTo(randomLocation.x(), randomLocation.y(), randomLocation.z());
	}

	private void onFail(ServerPlayer player) {
		getScoreAccess(objectives.fail(), player).add(1);
		getScoreAccess(objectives.stats(), FAIL_STAT_NAME).add(1);
		Vec3 randomLocation = getRandomLocation(player, failLocations);
		player.teleportTo(randomLocation.x(), randomLocation.y(), randomLocation.z());
		player.level().playSeededSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BANJO, SoundSource.VOICE, 1f, 0.01f, player.getRandom().nextLong());
	}

	public ScoreAccess getScoreAccess(Objective objective, ServerPlayer player) {
		return objective.getScoreboard().getOrCreatePlayerScore(player, objective);
	}

	public ScoreAccess getScoreAccess(Objective objective, String name) {
		return objective.getScoreboard().getOrCreatePlayerScore(ScoreHolder.forNameOnly(name), objective);
	}

	private Vec3 getRandomLocation(ServerPlayer serverPlayer, List<Vec3> locations) {
		int randomIndex = serverPlayer.getRandom().nextInt(locations.size());
		return locations.get(randomIndex);
	}

	private PlayerTeam getOrCreateTeam(ServerLevel level, String teamName) {
		PlayerTeam team = level.getScoreboard().getPlayerTeam(teamName);
		if (team == null) {
			team = level.getScoreboard().addPlayerTeam(teamName);
			team.setSeeFriendlyInvisibles(true);
		}
		return team;
	}

	@Override
	public Supplier<? extends PersistentGameBehaviorType<?>> type() {
		return PersistentGameBehaviors.PARKOUR;
	}

	public record Objectives(Objective win, Objective fail, Objective play, Objective stats) {

		public static Objectives createObjectives(ServerLevel level, String id) {
			String baseObjectiveName = "world_game.dropper." + id + ".stats.";
			return new Objectives(
					getOrCreate(level, baseObjectiveName + "wins"),
					getOrCreate(level, baseObjectiveName + "fails"),
					getOrCreate(level, baseObjectiveName + "plays"),
					getOrCreate(level, baseObjectiveName + "stats")
			);
		}

		private static Objective getOrCreate(ServerLevel level, String objectiveName) {
			Objective objective = level.getScoreboard().getObjective(objectiveName);
			if (objective == null) {
				objective = level.getScoreboard().addObjective(objectiveName, ObjectiveCriteria.DUMMY, Component.literal(objectiveName), ObjectiveCriteria.RenderType.INTEGER, true, null);
			}
			return objective;
		}
	}

	public record Reward(Optional<ItemStackTemplate> item, Optional<Identifier> collectible) {

		public static Reward NONE = new Reward(Optional.empty(), Optional.empty());

		public static Codec<Reward> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ItemStackTemplate.CODEC.optionalFieldOf("item").forGetter(Reward::item),
				Identifier.CODEC.optionalFieldOf("collectible").forGetter(Reward::collectible)
		).apply(instance, Reward::new));

		public void giveToPlayer(ServerPlayer player) {
			item.ifPresent(template -> player.addItem(template.create()));
			collectible.ifPresent(id -> GameRewards.grantCollectible(player, id));
		}
	}
}
