package com.lovetropics.minigames.common.core.game.behavior.instances.statistics;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.instances.action.SetStatisticAction;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.game.client_state.instance.StatisticOverlayState;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticsMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.Optional;
import java.util.function.Supplier;

public record StatisticOverlayBehavior(
		StatisticKey<Integer> statistic,
		ItemStackTemplate icon,
		Optional<String> translationKey,
		int valueChangeRate,
		SetStatisticAction.Scope scope
) implements IGameBehavior {
	public static final MapCodec<StatisticOverlayBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			StatisticKey.INT_CODEC.fieldOf("statistic").forGetter(StatisticOverlayBehavior::statistic),
			ItemStackTemplate.CODEC.fieldOf("icon").forGetter(StatisticOverlayBehavior::icon),
			Codec.STRING.optionalFieldOf("translation_key").forGetter(StatisticOverlayBehavior::translationKey),
			Codec.INT.optionalFieldOf("value_change_rate", 4).forGetter(StatisticOverlayBehavior::valueChangeRate),
			SetStatisticAction.Scope.CODEC.optionalFieldOf("scope", SetStatisticAction.Scope.PLAYER).forGetter(StatisticOverlayBehavior::scope)
	).apply(i, StatisticOverlayBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		GameClientState.applyGlobally(game, events, 4, GameClientStateTypes.STATISTIC_OVERLAY.get(), player -> {
			StatisticsMap statistics = scope.get(game, player);
			int value = statistics != null ? statistics.getInt(statistic) : 0;
			if (value == 0 && game.getRoleFor(player) != PlayerRole.PARTICIPANT) {
				return null;
			}
			return new StatisticOverlayState(icon.create(), translationKey, valueChangeRate, value);
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.STATISTIC_OVERLAY;
	}
}
