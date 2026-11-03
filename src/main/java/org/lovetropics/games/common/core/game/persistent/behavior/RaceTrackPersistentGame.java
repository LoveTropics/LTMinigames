package org.lovetropics.games.common.core.game.persistent.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.instances.action.RunCommandsAction;
import org.lovetropics.games.common.core.game.persistent.PersistentGame;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehavior;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehaviorType;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehaviors;
import org.lovetropics.games.common.core.game.persistent.PersistentScoreboards;
import org.lovetropics.games.common.core.game.region.RegionQuery;
import org.lovetropics.games.common.core.game.state.DebugModeState;
import org.lovetropics.games.common.util.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class RaceTrackPersistentGame implements PersistentGameBehavior {

	public static final MapCodec<RaceTrackPersistentGame> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.STRING.fieldOf("id").forGetter(b -> b.id),
			LootItemCondition.TYPED_CODEC.fieldOf("predicate").forGetter(b -> b.predicate),
			Codec.INT.fieldOf("checkpoint_count").forGetter(b -> b.checkpointCount),
			Codec.INT.fieldOf("required_checkpoint_count").forGetter(b -> b.checkpointCount),
			Messages.CODEC.fieldOf("messages").forGetter(b -> b.messages),
			Codec.BOOL.optionalFieldOf("only_start_at_first_checkpoint", true).forGetter(b -> b.onlyStartAtFirstCheckpoint),
			RegionQuery.CODEC.optionalFieldOf("leave_track_spawn").forGetter(b -> b.leaveTrackSpawn),
			ExtraCodecs.compactListCodec(RunCommandsAction.COMMAND_CODEC).optionalFieldOf("commands_on_leave", List.of()).forGetter(b -> b.commandOnLeave)
			).apply(instance, RaceTrackPersistentGame::new));

	private final String id;
	private final int checkpointCount;
	private final int requiredCheckpointCount;
	private final RegionQuery endRegionQuery;
	private final LootItemCondition predicate;
	private final Messages messages;
	private final boolean onlyStartAtFirstCheckpoint;
	private final Optional<RegionQuery> leaveTrackSpawn;
	private final List<String> commandOnLeave;

	private final Map<Integer, RegionQuery> regionQueries;
	private final Map<ServerPlayer, List<Integer>> playerCheckpoints;
	private final Map<ServerPlayer, Integer> playerStartTime;

	public RaceTrackPersistentGame(String id, LootItemCondition predicate, int checkpointCount, int requiredCheckpointCount, Messages messages, boolean onlyStartAtFirstCheckpoint, Optional<RegionQuery> leaveTrackSpawn, List<String> commandOnLeave) {
		this.id = id;
		this.predicate = predicate;
		this.checkpointCount = checkpointCount;
		this.requiredCheckpointCount = requiredCheckpointCount;
		this.messages = messages;
		this.endRegionQuery = RegionQuery.of(id + "_end");
		this.onlyStartAtFirstCheckpoint = onlyStartAtFirstCheckpoint;
		this.leaveTrackSpawn = leaveTrackSpawn;
		this.regionQueries = new HashMap<>();
		this.playerCheckpoints = new HashMap<>();
		this.playerStartTime = new HashMap<>();
		this.commandOnLeave = commandOnLeave;
	}

	@Override
	public void register(PersistentGame game, EventRegistrar events) {
		for (int i = 0; i < checkpointCount; i++) {
			regionQueries.put(i, RegionQuery.of(id + "_" + i));
		}

		events.listen(GamePhaseEvents.TICK, () -> {
			for (ServerPlayer player : game.level().players()) {
				if (checkPredicate(player)) {

					if (endRegionQuery.contains(game, player)) {
						List<Integer> checkpoints = playerCheckpoints.get(player);
						if (checkpoints == null || checkpoints.isEmpty()) {
							return;
						}
						if (checkpoints.size() >= requiredCheckpointCount) {
							long time = game.server().getTickCount() - playerStartTime.getOrDefault(player, 0);
							ScoreAccess scoreAccess = getScoreAccess(player, ScoreType.RECORD);
							if (scoreAccess.get() < time) {
								scoreAccess.set(Math.toIntExact(time));
							}
							getScoreAccess(player, ScoreType.LATEST).set(Math.toIntExact(time));
							player.sendSystemMessage(messages.raceCompleted, false);
							playerCheckpoints.remove(player);
						} else {
							player.sendSystemMessage(messages.notEnoughCheckpoints, false);
							playerCheckpoints.remove(player);
						}
					}


					for (int i = 0; i < checkpointCount; i++) {
						RegionQuery query = regionQueries.get(i);
						if (query != null && query.contains(game, player)) {
							if (!playerCheckpoints.containsKey(player)) {
								if (onlyStartAtFirstCheckpoint && i != 0) {
									messages.notFirstCheckpoint.ifPresent(component -> player.sendSystemMessage(component, false));
									continue;
								}
								playerCheckpoints.put(player, new ArrayList<>());
								playerStartTime.put(player, game.server().getTickCount());
							}
							List<Integer> checkpoints = playerCheckpoints.get(player);
							if (!checkpoints.contains(i)) {
								player.sendSystemMessage(messages.checkpointReached, true);
								Util.sendNotifySound(player, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
								checkpoints.add(i);
							}
						}
					}


				} else {
					if (playerCheckpoints.containsKey(player)) {
						playerCheckpoints.remove(player);
						player.sendSystemMessage(messages.leftRaceTrack, false);
						leaveTrackSpawn.ifPresent(query -> {
							Vec3 center = query.one(game).center();
							player.teleportTo(center.x, center.y, center.z);
						});
						if (!commandOnLeave.isEmpty()) {
							Commands commands = game.server().getCommands();
							CommandSourceStack targetSource = createCommandSourceForEntity(game, player);
							for (String command : commandOnLeave) {
								commands.performPrefixedCommand(targetSource, command);
							}
						}
					}
				}
			}
		});
	}

	private static CommandSourceStack createCommandSourceForEntity(PersistentGame game, ServerPlayer entity) {
		return new CommandSourceStack(
				entity.commandSource(),
				entity.position(),
				entity.getRotationVector(),
				entity.level(),
				LevelBasedPermissionSet.OWNER,
				entity.getPlainTextName(),
				entity.getDisplayName(),
				game.server(),
				entity
		);
	}

	public enum ScoreType {
		RECORD,
		LATEST,
	}

	private ScoreAccess getScoreAccess(ServerPlayer serverPlayer, ScoreType scoreType) {
		Objective objective = getScoreObjective(serverPlayer, scoreType.name().toLowerCase());
		return objective.getScoreboard().getOrCreatePlayerScore(serverPlayer, objective);
	}

	private Objective getScoreObjective(ServerPlayer serverPlayer, String type) {
		return PersistentScoreboards.getOrCreateObjective(serverPlayer.level(), "race_track", id, type);
	}

	private boolean checkPredicate(ServerPlayer serverPlayer) {
		LootParams lootParams = new LootParams.Builder(serverPlayer.level())
				.withParameter(LootContextParams.ORIGIN, serverPlayer.position())
				.withOptionalParameter(LootContextParams.THIS_ENTITY, serverPlayer)
				.create(LootContextParamSets.COMMAND);

		LootContext context = new LootContext.Builder(lootParams).create(Optional.empty());
		context.pushVisitedElement(LootContext.createVisitedEntry(predicate));

		return predicate.test(context);
	}

	@Override
	public Supplier<? extends PersistentGameBehaviorType<?>> type() {
		return PersistentGameBehaviors.RACE_TRACK;
	}
	
	public record Messages(Component checkpointReached, Component raceCompleted, Component notEnoughCheckpoints, Component leftRaceTrack, Optional<Component> notFirstCheckpoint) {
		public static final MapCodec<Messages> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				ComponentSerialization.CODEC.fieldOf("checkpoint_reached").forGetter(Messages::checkpointReached),
				ComponentSerialization.CODEC.fieldOf("race_completed").forGetter(Messages::raceCompleted),
				ComponentSerialization.CODEC.fieldOf("not_enough_checkpoints").forGetter(Messages::notEnoughCheckpoints),
				ComponentSerialization.CODEC.fieldOf("left_race_track").forGetter(Messages::leftRaceTrack),
				ComponentSerialization.CODEC.optionalFieldOf("not_first_checkpoint").forGetter(Messages::notFirstCheckpoint)
		).apply(instance, Messages::new));
	}
}
