package com.lovetropics.minigames.common.core.game.behavior.instances.statistics;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContextKeys;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;

import java.util.UUID;

public record StatisticNotifierBehavior(
		StatisticKey<Integer> statistic,
		boolean onlyIncrease,
		GameActionList actions
) implements IGameBehavior {
	public static final MapCodec<StatisticNotifierBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			StatisticKey.INT_CODEC.fieldOf("statistic").forGetter(StatisticNotifierBehavior::statistic),
			Codec.BOOL.optionalFieldOf("only_increase", true).forGetter(StatisticNotifierBehavior::onlyIncrease),
			GameActionList.MAP_CODEC.forGetter(StatisticNotifierBehavior::actions)
	).apply(i, StatisticNotifierBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		actions.register(game, events);

		Object2IntMap<UUID> lastValues = new Object2IntOpenHashMap<>();

		events.listen(GamePlayerEvents.ADD, player ->
				// We don't want to notify players about statistics they already had when they joined
				lastValues.put(player.getUUID(), game.statistics().forPlayer(player).getInt(statistic))
		);
		events.listen(GamePlayerEvents.REMOVE, player -> lastValues.removeInt(player.getUUID()));

		events.listen(GamePhaseEvents.TICK, () -> {
			for (ServerPlayer player : game.allPlayers()) {
				int value = game.statistics().forPlayer(player).getInt(statistic);
				int lastValue = lastValues.put(player.getUUID(), value);
				if (onlyIncrease && value < lastValue) {
					return;
				}
				if (lastValue != value) {
					ContextMap context = new ContextMap.Builder()
							.withParameter(GameActionContextKeys.CHANGE, value - lastValue)
							.create(ContextKeySet.EMPTY);
					actions.apply(game, context, ActionSubjects.ofPlayer(player));
				}
			}
		});
	}
}
