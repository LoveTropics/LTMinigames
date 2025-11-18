package com.lovetropics.minigames.common.content.river_race.behaviour;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.function.Supplier;

public record KillInVoidBehavior(String voidBelowRegionKey) implements IGameBehavior {
	public static final MapCodec<KillInVoidBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("void_below_region").forGetter(KillInVoidBehavior::voidBelowRegionKey)
	).apply(i, KillInVoidBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		List<BlockBox> regions = game.mapRegions().getAll(voidBelowRegionKey);
		if (regions.isEmpty()) {
			throw new GameException(Component.literal("No " + voidBelowRegionKey + " region"));
		}

		if (regions.size() == 1) {
			int minY = regions.getFirst().min().getY();
			events.listen(GamePlayerEvents.TICK, player -> {
				if (!shouldIgnorePlayer(player) && player.getY() < minY) {
					player.kill(game.level());
				}
			});
			return;
		}

		events.listen(GamePlayerEvents.TICK, player -> {
			if (shouldIgnorePlayer(player)) {
				return;
			}
			BlockPos playerPos = player.blockPosition();
			int closestDistance = Integer.MAX_VALUE;
			BlockBox closestRegion = null;
			for (BlockBox region : regions) {
				int distance = getDistanceToEdge(region, playerPos.getX(), playerPos.getZ());
				if (distance < closestDistance) {
					closestRegion = region;
					closestDistance = distance;
					if (distance == 0) {
						break;
					}
				}
			}
			if (closestRegion != null && player.getY() < closestRegion.min().getY()) {
				player.kill(game.level());
			}
		});
	}

	private boolean shouldIgnorePlayer(ServerPlayer player) {
		return player.isSpectator() || player.isCreative();
	}

	private int getDistanceToEdge(BlockBox region, int playerX, int playerZ) {
		int nearestX = Mth.clamp(playerX, region.min().getX(), region.max().getX());
		int nearestZ = Mth.clamp(playerZ, region.min().getZ(), region.max().getZ());
		return Math.abs(nearestX - playerX) + Math.abs(nearestZ - playerZ);
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.KILL_IN_VOID;
	}
}
