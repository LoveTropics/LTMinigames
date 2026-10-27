package org.lovetropics.games.common.content.treasure_dig;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamState;
import org.lovetropics.games.common.util.Util;

import java.util.Map;

public record TreasureDigBehaviour(
		Map<Holder<Item>, Integer> pointsMap
) implements IGameBehavior {
	public static final MapCodec<TreasureDigBehaviour> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.unboundedMap(BuiltInRegistries.ITEM.holderByNameCodec(), Codec.INT).optionalFieldOf("points_map", Map.of()).forGetter(b -> b.pointsMap)
	).apply(i, TreasureDigBehaviour::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);

		events.listen(GamePlayerEvents.INVENTORY_CHANGED, (player, container, slotIndex, newItemStack) -> {
			GameTeamKey team = teams.getTeamForPlayer(player);
			if (team == null) {
				return;
			}

			int oldPlayerScore = game.statistics().forPlayer(player).getInt(StatisticKey.POINTS);
			int newPlayerScore = calculatePlayerScore(player);
			if (newPlayerScore == oldPlayerScore) {
				return;
			}
			game.statistics().forPlayer(player).set(StatisticKey.POINTS, newPlayerScore);
			Util.sendNotifySound(player, SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1.0f, 1.0f);

			game.statistics().forTeam(team).set(StatisticKey.POINTS, sumTeamScore(game, teams, team));
		});
	}

	private int sumTeamScore(IGamePhase game, TeamState teams, GameTeamKey team) {
		int teamScore = 0;
		for (ServerPlayer player : teams.getPlayersForTeam(game, team)) {
			teamScore += game.statistics().forPlayer(player).getInt(StatisticKey.POINTS);
		}
		return teamScore;
	}

	private int calculatePlayerScore(ServerPlayer serverPlayer) {
		int score = 0;
		for (ItemStack itemStack : serverPlayer.getInventory()) {
			if (itemStack.isEmpty()) {
				continue;
			}
			score += pointsMap().getOrDefault(itemStack.typeHolder(), 0) * itemStack.getCount();
		}
		return score;
	}
}
