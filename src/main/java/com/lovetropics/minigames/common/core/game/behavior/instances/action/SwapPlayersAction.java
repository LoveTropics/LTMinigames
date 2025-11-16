package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public record SwapPlayersAction(double distanceThreshold) implements IGameBehavior {
	public static final MapCodec<SwapPlayersAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.DOUBLE.optionalFieldOf("distance_threshold", Double.MAX_VALUE).forGetter(c -> c.distanceThreshold)
	).apply(i, SwapPlayersAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			if (game.participants().size() <= 1) {
				return false;
			}
			if (distanceThreshold == Double.MAX_VALUE) {
				shufflePlayers(game);
			} else {
				swapNearbyPlayers(game);
			}
			return true;
		});
	}

	private void shufflePlayers(IGamePhase game) {
		List<ServerPlayer> players = game.participants().shuffledCopy(game.random());

		List<Vec3> playerPositions = players.stream()
				.map(Entity::position)
				.toList();

		for (int i = 0; i < players.size(); i++) {
			final ServerPlayer player = players.get(i);
			final Vec3 teleportTo = playerPositions.get((i + 1) % playerPositions.size());
			player.teleportTo(teleportTo.x, teleportTo.y, teleportTo.z);
		}
	}

	private void swapNearbyPlayers(IGamePhase game) {
		List<ServerPlayer> players = game.participants().shuffledCopy(game.random());

		List<Vec3> playerPositions = players.stream()
				.map(Entity::position)
				.toList();

		double distanceThreshold2 = distanceThreshold * distanceThreshold;

		for (ServerPlayer player : players) {
			Vec3 closestPos = null;
			double closestDistance2 = Double.MAX_VALUE;

			for (Vec3 otherPos : playerPositions) {
				double distance2 = player.position().distanceToSqr(otherPos);
				if (distance2 > 0.01 && distance2 < distanceThreshold2) {
					if (distance2 < closestDistance2) {
						closestPos = otherPos;
						closestDistance2 = distance2;
					}
				}
			}

			if (closestPos != null) {
				player.teleportTo(closestPos.x, closestPos.y, closestPos.z);
			}
		}
	}
}
