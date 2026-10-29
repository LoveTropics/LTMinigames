package org.lovetropics.games.common.content.river_race.behaviour;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.region.Region;
import org.lovetropics.games.common.core.game.region.RegionQuery;

public record KillInVoidBehavior(RegionQuery voidBelowRegion) implements IGameBehavior {
	public static final MapCodec<KillInVoidBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegionQuery.CODEC.fieldOf("void_below_region").forGetter(KillInVoidBehavior::voidBelowRegion)
	).apply(i, KillInVoidBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GamePlayerEvents.TICK, player -> {
			if (shouldIgnorePlayer(player)) {
				return;
			}
			BlockPos playerPos = player.blockPosition();
			Region closestRegion = findRegion(game, player, playerPos);
			if (closestRegion != null && player.getY() < closestRegion.min().getY()) {
				player.kill(player.level());
			}
		});
	}

	private @Nullable Region findRegion(IGamePhase game, ServerPlayer player, BlockPos playerPos) {
		int closestDistance = Integer.MAX_VALUE;
		Region closestRegion = null;
		for (Region region : voidBelowRegion.allIn(game, player.level())) {
			int distance = getDistanceToEdge(region, playerPos.getX(), playerPos.getZ());
			if (distance < closestDistance) {
				closestRegion = region;
				closestDistance = distance;
				if (distance == 0) {
					return region;
				}
			}
		}
		return closestRegion;
	}

	private boolean shouldIgnorePlayer(ServerPlayer player) {
		return player.isSpectator() || player.isCreative();
	}

	private int getDistanceToEdge(Region region, int playerX, int playerZ) {
		int nearestX = Mth.clamp(playerX, region.min().getX(), region.max().getX());
		int nearestZ = Mth.clamp(playerZ, region.min().getZ(), region.max().getZ());
		return Math.abs(nearestX - playerX) + Math.abs(nearestZ - playerZ);
	}

	@Override
	public MapCodec<? extends IGameBehavior> codec() {
		return CODEC;
	}
}
