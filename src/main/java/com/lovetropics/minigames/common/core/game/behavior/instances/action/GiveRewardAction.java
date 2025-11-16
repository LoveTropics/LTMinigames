package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.rewards.GameRewardsMap;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public record GiveRewardAction(List<ItemStack> items, Optional<StatisticBinding> statisticBinding) implements IGameBehavior {
	public static final MapCodec<GiveRewardAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			MoreCodecs.ITEM_STACK.listOf().fieldOf("items").forGetter(GiveRewardAction::items),
			StatisticBinding.CODEC.optionalFieldOf("statistic_binding").forGetter(GiveRewardAction::statisticBinding)
	).apply(i, GiveRewardAction::new));

	@Override
	public void register(final IGamePhase game, final EventRegistrar events) throws GameException {
		GameRewardsMap rewards = game.instanceState().getOrThrow(GameRewardsMap.STATE);
		events.applyToPlayers(game, (context, target) -> {
			for (final ItemStack item : items) {
				final int count = statisticBinding.map(binding -> binding.resolve(game, target)).orElse(item.getCount());
				rewards.forPlayer(target).give(item.copyWithCount(count));
			}
			return true;
		});
	}

	public record StatisticBinding(StatisticKey<Integer> statistic, float multiplier, boolean fromTeam) {
		public static final Codec<StatisticBinding> CODEC = RecordCodecBuilder.create(i -> i.group(
				StatisticKey.INT_CODEC.fieldOf("statistic").forGetter(StatisticBinding::statistic),
				Codec.FLOAT.optionalFieldOf("multiplier", 1.0f).forGetter(StatisticBinding::multiplier),
				Codec.BOOL.optionalFieldOf("from_team", false).forGetter(StatisticBinding::fromTeam)
		).apply(i, StatisticBinding::new));

		public int resolve(final IGamePhase game, final ServerPlayer player) {
			float value = 0.0f;
			if (fromTeam) {
				final TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);
				final GameTeamKey team = teams.getTeamForPlayer(player);
				if (team != null) {
					value += game.statistics().forTeam(team).getInt(statistic);
				}
			} else {
				value = game.statistics().forPlayer(player).getInt(statistic);
			}
			return Mth.floor(multiplier * value);
		}
	}
}
