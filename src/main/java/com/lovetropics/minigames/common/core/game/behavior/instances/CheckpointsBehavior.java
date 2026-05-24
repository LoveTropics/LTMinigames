package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.content.MinigameTexts;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.SpawnBuilder;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public record CheckpointsBehavior(
		List<CheckpointConfig> checkpointConfigs,
		float angle,
		boolean teams
) implements IGameBehavior {
	public static final MapCodec<CheckpointsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			CheckpointConfig.CODEC.listOf().fieldOf("checkpoints").forGetter(CheckpointsBehavior::checkpointConfigs),
			Codec.FLOAT.optionalFieldOf("angle", 0.0f).forGetter(CheckpointsBehavior::angle),
			Codec.BOOL.optionalFieldOf("teams", false).forGetter(CheckpointsBehavior::teams)
	).apply(i, CheckpointsBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		List<Checkpoint> checkpoints = new ArrayList<>();
		for (int i = 0; i < checkpointConfigs.size(); i++) {
			CheckpointConfig config = checkpointConfigs.get(i);
			config.reachedActions.register(game, events);
			checkpoints.addAll(config.resolve(game, i));
		}

		if (teams) {
			TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);
			Map<GameTeamKey, Checkpoint> lastCheckpointByTeam = new HashMap<>();

			events.listen(GamePlayerEvents.TICK, player -> {
				GameTeamKey team = teams.getTeamForPlayer(player);
				if (team == null) {
					return;
				}
				Checkpoint atCheckpoint = getCheckpointAt(player.getBoundingBox(), checkpoints);
				if (atCheckpoint == null) {
					return;
				}
				Checkpoint lastCheckpoint = lastCheckpointByTeam.get(team);
				if (lastCheckpoint != atCheckpoint && (lastCheckpoint == null || atCheckpoint.group.order >= lastCheckpoint.group.order)) {
					PlayerSet teamPlayers = teams.getPlayersForTeam(game, team);
					teamPlayers.playSound(SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1.0f, 1.0f);
					teamPlayers.sendMessage(MinigameTexts.CHECKPOINT_REACHED, true);
					lastCheckpointByTeam.put(team, atCheckpoint);
					atCheckpoint.group.reachedActions.apply(game, ContextMap.EMPTY, ActionSubjects.ofTeam(team));
				}
			});

			events.listen(GamePlayerEvents.SPAWN, (playerId, spawn, role) -> {
				GameTeamKey team = teams.getTeamForPlayer(playerId);
				Checkpoint checkpoint = team != null ? lastCheckpointByTeam.get(team) :  null;
				if (checkpoint != null) {
					spawnAt(game, spawn, checkpoint);
				}
			});
		} else {
			Map<UUID, Checkpoint> lastCheckpointByPlayer = new HashMap<>();

			events.listen(GamePlayerEvents.TICK, player -> {
				Checkpoint atCheckpoint = getCheckpointAt(player.getBoundingBox(), checkpoints);
				if (atCheckpoint == null) {
					return;
				}
				Checkpoint lastCheckpoint = lastCheckpointByPlayer.get(player.getUUID());
				if (lastCheckpoint != atCheckpoint && (lastCheckpoint == null || atCheckpoint.group.order >= lastCheckpoint.group.order)) {
					com.lovetropics.minigames.common.util.Util.sendNotifySound(player, SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1.0f, 1.0f);
					player.sendSystemMessage(MinigameTexts.CHECKPOINT_REACHED, true);
					lastCheckpointByPlayer.put(player.getUUID(), atCheckpoint);
					atCheckpoint.group.reachedActions.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player));
				}
			});

			events.listen(GamePlayerEvents.SPAWN, (playerId, spawn, role) -> {
				Checkpoint checkpoint = lastCheckpointByPlayer.get(playerId);
				if (checkpoint != null) {
					spawnAt(game, spawn, checkpoint);
				}
			});
		}
	}

	private void spawnAt(IGamePhase game, SpawnBuilder spawn, Checkpoint checkpoint) {
		BlockBox spawnBox = checkpoint.group.spawns != null ? Util.getRandom(checkpoint.group.spawns, game.random()) : checkpoint.box;
		Vec3 spawnPos = spawnBox.center().with(Direction.Axis.Y, spawnBox.min().getY());
		BlockBox faceRegion = checkpoint.group.faceRegion;
		float angle = faceRegion != null ? PositionPlayersBehavior.getAngleTo(BlockPos.containing(spawnPos), faceRegion) : this.angle;
		spawn.teleportTo(game.level(), spawnPos, angle, 0.0f);
	}

	private @Nullable Checkpoint getCheckpointAt(AABB aabb, List<Checkpoint> checkpoints) {
		for (Checkpoint checkpoint : checkpoints) {
			if (checkpoint.box.intersects(aabb)) {
				return checkpoint;
			}
		}
		return null;
	}

	private record CheckpointConfig(
			String region,
			Optional<String> spawnRegion,
			Optional<String> faceRegion,
			GameActionList reachedActions
	) {
		public static final Codec<CheckpointConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("region").forGetter(CheckpointConfig::region),
				Codec.STRING.optionalFieldOf("spawn_region").forGetter(CheckpointConfig::spawnRegion),
				Codec.STRING.optionalFieldOf("face_region").forGetter(CheckpointConfig::faceRegion),
				GameActionList.CODEC.optionalFieldOf("reached_actions", GameActionList.EMPTY).forGetter(CheckpointConfig::reachedActions)
		).apply(i, CheckpointConfig::new));

		public List<Checkpoint> resolve(IGamePhase game, int order) {
			List<BlockBox> checkpointRegions = game.mapRegions().getAllOrThrow(region);
			List<BlockBox> spawns = spawnRegion.map(game.mapRegions()::getAllOrThrow).orElse(null);
			BlockBox faceRegion = this.faceRegion.map(game.mapRegions()::getOrThrow).orElse(null);
			CheckpointGroup group = new CheckpointGroup(order, spawns, faceRegion, reachedActions);
			return checkpointRegions.stream()
					.map(box -> new Checkpoint(group, box))
					.toList();
		}
	}

	private record CheckpointGroup(
			int order,
			@Nullable List<BlockBox> spawns,
			@Nullable BlockBox faceRegion,
			GameActionList reachedActions
	) {
	}

	private record Checkpoint(CheckpointGroup group, BlockBox box) {
	}
}
