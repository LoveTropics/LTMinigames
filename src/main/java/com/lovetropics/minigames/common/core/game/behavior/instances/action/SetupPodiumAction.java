package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.behavior.instances.PositionPlayersBehavior;
import com.lovetropics.minigames.common.core.game.state.statistics.GameStatistics;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;

import java.util.List;
import java.util.Set;

public record SetupPodiumAction(
		List<String> winnerRegions,
		String loserRegion
) implements IGameBehavior {
	public static final MapCodec<SetupPodiumAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ExtraCodecs.nonEmptyList(Codec.STRING.listOf()).fieldOf("winner_regions").forGetter(SetupPodiumAction::winnerRegions),
			Codec.STRING.fieldOf("loser_region").forGetter(SetupPodiumAction::loserRegion)
	).apply(i, SetupPodiumAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		List<BlockBox> winnerBoxes = winnerRegions.stream().map(game.mapRegions()::getOrThrow).toList();
		BlockBox loserBox = game.mapRegions().getOrThrow(loserRegion);

		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			setupPodium(game, winnerBoxes, loserBox);
			return true;
		});
	}

	private void setupPodium(IGamePhase game, List<BlockBox> winnerBoxes, BlockBox loserBox) {
		GameStatistics statistics = game.statistics();

		List<ServerPlayer> remainingPlayers = game.allPlayers().shuffledCopy(game.random());

		for (PlayerKey playerKey : statistics.getPlayers()) {
			ServerPlayer player = game.allPlayers().getPlayerBy(playerKey);
			if (player == null) {
				continue;
			}

			int placement = statistics.forPlayer(playerKey).getOr(StatisticKey.PLACEMENT, Integer.MAX_VALUE);
			if (placement >= 1 && placement <= winnerBoxes.size()) {
				teleportToRegion(game, player, winnerBoxes.get(placement - 1), loserBox);
				remainingPlayers.remove(player);
			}
		}

		for (ServerPlayer player : remainingPlayers) {
			teleportToRegion(game, player, loserBox, winnerBoxes.getFirst());
		}
	}

	private static void teleportToRegion(IGamePhase game, ServerPlayer player, BlockBox region, BlockBox facingRegion) {
		BlockPos pos = PositionPlayersBehavior.tryFindEmptyPos(game, player.getRandom(), region);
		float angle = PositionPlayersBehavior.getAngleTo(pos, facingRegion);
		player.teleportTo(player.level(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Set.of(), angle, 0.0f, true);
	}
}
