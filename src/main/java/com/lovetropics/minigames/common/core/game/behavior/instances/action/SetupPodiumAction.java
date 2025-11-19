package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.player.MutablePlayerSet;
import com.lovetropics.minigames.common.core.game.state.statistics.GameStatistics;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

public record SetupPodiumAction(
		List<String> winnerRegions,
		String loserRegion
) implements IGameBehavior {
	public static final MapCodec<SetupPodiumAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.listOf().fieldOf("winner_regions").forGetter(SetupPodiumAction::winnerRegions),
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

		MutablePlayerSet players = new MutablePlayerSet(game.server());
		game.allPlayers().forEach(players::add);

		for (ServerPlayer player : players) {
			game.setPlayerRole(player, null);
		}

		for (PlayerKey playerKey : statistics.getPlayers()) {
			ServerPlayer player = players.getPlayerBy(playerKey);
			if (player == null) {
				continue;
			}

			int placement = statistics.forPlayer(playerKey).getOr(StatisticKey.PLACEMENT, Integer.MAX_VALUE);
			if (placement >= 1 && placement <= winnerBoxes.size()) {
				teleportToRegion(player, winnerBoxes.get(placement - 1));
				players.remove(playerKey.id());
			}
		}

		for (ServerPlayer player : players) {
			teleportToRegion(player, loserBox);
		}
	}

	private static void teleportToRegion(ServerPlayer player, BlockBox region) {
		Vec3 pos = region.center();
		player.teleportTo(player.level(), pos.x(), pos.y(), pos.z(), Set.of(), 0.0f, 0.0f, true);
	}
}
