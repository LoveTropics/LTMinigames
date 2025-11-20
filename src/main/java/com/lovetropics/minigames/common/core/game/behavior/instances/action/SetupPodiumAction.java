package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
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
import net.minecraft.util.context.ContextMap;

import java.util.List;
import java.util.Set;

public record SetupPodiumAction(
		List<String> winnerRegions,
		List<GameActionList> winnerActions,
		String loserRegion,
		GameActionList loserActions
) implements IGameBehavior {
	public static final MapCodec<SetupPodiumAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ExtraCodecs.nonEmptyList(Codec.STRING.listOf()).fieldOf("winner_regions").forGetter(SetupPodiumAction::winnerRegions),
			GameActionList.CODEC.listOf().optionalFieldOf("winner_actions", List.of()).forGetter(SetupPodiumAction::winnerActions),
			Codec.STRING.fieldOf("loser_region").forGetter(SetupPodiumAction::loserRegion),
			GameActionList.CODEC.optionalFieldOf("loser_actions", GameActionList.EMPTY).forGetter(SetupPodiumAction::loserActions)
	).apply(i, SetupPodiumAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		for (GameActionList actions : winnerActions) {
			actions.register(game, events);
		}
		loserActions.register(game, events);

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
				if (placement <= winnerActions.size()) {
					winnerActions.get(placement - 1).apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player));
				}
				remainingPlayers.remove(player);
			}
		}

		for (ServerPlayer player : remainingPlayers) {
			teleportToRegion(game, player, loserBox, winnerBoxes.getFirst());
		}
		loserActions.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayers(remainingPlayers));
	}

	private static void teleportToRegion(IGamePhase game, ServerPlayer player, BlockBox region, BlockBox facingRegion) {
		BlockPos pos = PositionPlayersBehavior.tryFindEmptyPos(game, player.getRandom(), region);
		float angle = PositionPlayersBehavior.getAngleTo(pos, facingRegion);
		player.teleportTo(game.level(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, Set.of(), angle, 0.0f, true);
	}
}
