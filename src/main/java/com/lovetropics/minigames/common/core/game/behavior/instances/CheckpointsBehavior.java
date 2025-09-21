package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.content.MinigameTexts;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public record CheckpointsBehavior(
		String regionKey,
		float angle
) implements IGameBehavior {
	public static final MapCodec<CheckpointsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.optionalFieldOf("region", "checkpoint").forGetter(CheckpointsBehavior::regionKey),
			Codec.FLOAT.optionalFieldOf("angle", 0.0f).forGetter(CheckpointsBehavior::angle)
	).apply(i, CheckpointsBehavior::new));

	private static final int NO_CHECKPOINT = -1;

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		List<BlockBox> checkpoints = game.mapRegions().getAll(regionKey);
		Object2IntMap<UUID> lastCheckpointByPlayer = new Object2IntOpenHashMap<>();
		lastCheckpointByPlayer.defaultReturnValue(NO_CHECKPOINT);

		events.listen(GamePlayerEvents.TICK, player -> {
			int atCheckpoint = getCheckpointAt(player.getBoundingBox(), checkpoints);
			if (atCheckpoint == NO_CHECKPOINT) {
				return;
			}
			int lastCheckpoint = lastCheckpointByPlayer.put(player.getUUID(), atCheckpoint);
			if (lastCheckpoint != atCheckpoint) {
				player.playNotifySound(SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1.0f, 1.0f);
				player.sendSystemMessage(MinigameTexts.CHECKPOINT_REACHED, true);
			}
		});

		events.listen(GamePlayerEvents.SPAWN, (playerId, spawn, role) -> {
			int checkpointId = lastCheckpointByPlayer.getInt(playerId);
			if (checkpointId == NO_CHECKPOINT) {
				return;
			}
			BlockBox checkpoint = checkpoints.get(checkpointId);
			Vec3 spawnPos = checkpoint.center().with(Direction.Axis.Y, checkpoint.min().getY());
			spawn.teleportTo(game.level(), spawnPos, angle, 0.0f);
		});
	}

	private int getCheckpointAt(AABB aabb, List<BlockBox> checkpoints) {
		for (int i = 0; i < checkpoints.size(); i++) {
			BlockBox checkpoint = checkpoints.get(i);
			if (checkpoint.intersects(aabb)) {
				return i;
			}
		}
		return NO_CHECKPOINT;
	}
}
