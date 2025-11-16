package com.lovetropics.minigames.common.content.qottott.behavior;

import com.lovetropics.minigames.common.content.qottott.Qottott;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContextKeys;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

import java.util.function.Supplier;

public record GivePointsAction(StatisticKey<Integer> statistic, int count, boolean bypassMultiplier) implements IGameBehavior {
	public static final MapCodec<GivePointsAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			StatisticKey.INT_CODEC.fieldOf("statistic").forGetter(GivePointsAction::statistic),
			Codec.INT.optionalFieldOf("count", 1).forGetter(GivePointsAction::count),
			Codec.BOOL.optionalFieldOf("bypass_multiplier", false).forGetter(GivePointsAction::bypassMultiplier)
	).apply(i, GivePointsAction::new));

	@Override
	public void register(final IGamePhase game, final EventRegistrar events) {
		events.applyToPlayers(game, (context, player) -> {
			final int count = resolveCount(context, player);
			if (count > 0) {
				game.statistics().forPlayer(player).incrementInt(statistic, count);
				return true;
			}
			return false;
		});
	}

	private int resolveCount(ContextMap context, ServerPlayer player) {
		if (bypassMultiplier) {
			return count;
		}
		final int count = this.count * context.getOrDefault(GameActionContextKeys.COUNT, 1);
		return Mth.floor(count * getMultiplier(player));
	}

	private static double getMultiplier(ServerPlayer player) {
		final AttributeInstance attribute = player.getAttribute(Qottott.COIN_MULTIPLIER);
		return attribute != null ? attribute.getValue() : 1.0;
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return Qottott.GIVE_POINTS;
	}
}
