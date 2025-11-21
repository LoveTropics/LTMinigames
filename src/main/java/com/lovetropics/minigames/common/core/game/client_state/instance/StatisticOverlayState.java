package com.lovetropics.minigames.common.core.game.client_state.instance;

import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.util.Codecs;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.Optional;

public record StatisticOverlayState(
		ItemStack icon,
		Optional<String> translationKey,
		int valueChangeRate,
		int value
) implements GameClientState {
	public static final MapCodec<StatisticOverlayState> CODEC = Codecs.no();
	public static final StreamCodec<RegistryFriendlyByteBuf, StatisticOverlayState> STREAM_CODEC = StreamCodec.composite(
			ItemStack.STREAM_CODEC, StatisticOverlayState::icon,
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs::optional), StatisticOverlayState::translationKey,
			ByteBufCodecs.VAR_INT, StatisticOverlayState::valueChangeRate,
			ByteBufCodecs.VAR_INT, StatisticOverlayState::value,
			StatisticOverlayState::new
	);

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.STATISTIC_OVERLAY.get();
	}

	public Ticker tick(@Nullable Ticker ticker) {
		if (ticker == null || !ticker.translationKey.equals(translationKey) || !ItemStack.matches(ticker.icon, icon)) {
			return new Ticker(icon, translationKey, value);
		}
		ticker.targetValue = value;
		ticker.tick(valueChangeRate);
		return ticker;
	}

	public static class Ticker {
		private final ItemStack icon;
		private final Optional<String> translationKey;

		private int value;
		private int targetValue;

		private Ticker(ItemStack icon, Optional<String> translationKey, int initialValue) {
			this.icon = icon;
			this.translationKey = translationKey;
			value = initialValue;
			targetValue = initialValue;
		}

		public void tick(int valueChangeRate) {
			if (value < targetValue) {
				value = Math.min(value + valueChangeRate, targetValue);
			} else if (value > targetValue) {
				value = Math.max(value - valueChangeRate, targetValue);
			}
		}

		public ItemStack icon() {
			return icon;
		}

		public Component text() {
			if (translationKey.isPresent()) {
				return Component.translatable(translationKey.get(), value);
			}
			return Component.literal(String.valueOf(value));
		}
	}
}
