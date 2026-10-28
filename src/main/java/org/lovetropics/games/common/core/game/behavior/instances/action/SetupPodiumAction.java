package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.context.ContextMap;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.behavior.instances.PositionPlayersBehavior;
import org.lovetropics.games.common.core.game.region.Region;
import org.lovetropics.games.common.core.game.region.RegionQuery;
import org.lovetropics.games.common.core.game.state.statistics.GameStatistics;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;

import java.util.List;
import java.util.Set;

public record SetupPodiumAction(
		List<RegionQuery> winnerRegions,
		List<GameActionList> winnerActions,
		RegionQuery loserRegion,
		GameActionList loserActions
) implements IGameBehavior {
	public static final MapCodec<SetupPodiumAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ExtraCodecs.nonEmptyList(RegionQuery.CODEC.listOf()).fieldOf("winner_regions").forGetter(SetupPodiumAction::winnerRegions),
			GameActionList.CODEC.listOf().optionalFieldOf("winner_actions", List.of()).forGetter(SetupPodiumAction::winnerActions),
			RegionQuery.CODEC.fieldOf("loser_region").forGetter(SetupPodiumAction::loserRegion),
			GameActionList.CODEC.optionalFieldOf("loser_actions", GameActionList.EMPTY).forGetter(SetupPodiumAction::loserActions)
	).apply(i, SetupPodiumAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		for (GameActionList actions : winnerActions) {
			actions.register(game, events);
		}
		loserActions.register(game, events);

		List<Region> winnerBoxes = winnerRegions.stream().map(r -> r.one(game)).toList();
		Region loserBox = loserRegion.one(game);

		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			setupPodium(game, winnerBoxes, loserBox);
			return true;
		});
	}

	private void setupPodium(IGamePhase game, List<Region> winnerBoxes, Region loserBox) {
		GameStatistics statistics = game.statistics();

		List<ServerPlayer> remainingPlayers = game.allPlayers().shuffledCopy(game.random());

		for (PlayerKey playerKey : statistics.getPlayers()) {
			ServerPlayer player = game.allPlayers().getPlayerBy(playerKey);
			if (player == null) {
				continue;
			}

			int placement = statistics.forPlayer(playerKey).getOr(StatisticKey.PLACEMENT, Integer.MAX_VALUE);
			if (placement >= 1 && placement <= winnerBoxes.size()) {
				teleportToRegion(player, winnerBoxes.get(placement - 1), loserBox);
				if (placement <= winnerActions.size()) {
					winnerActions.get(placement - 1).apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player));
				}
				remainingPlayers.remove(player);
			}
		}

		for (ServerPlayer player : remainingPlayers) {
			teleportToRegion(player, loserBox, winnerBoxes.getFirst());
		}
		loserActions.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayers(remainingPlayers));
	}

	private static void teleportToRegion(ServerPlayer player, Region region, Region facingRegion) {
		BlockPos pos = PositionPlayersBehavior.tryFindEmptyPos(player.getRandom(), region);
		float angle = PositionPlayersBehavior.getAngleTo(pos, facingRegion.box());
		player.teleportTo(region.level(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, Set.of(), angle, 0.0f, true);
	}
}
