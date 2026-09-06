package com.lovetropics.minigames.common.core.game.weather;

import net.minecraft.network.FriendlyByteBuf;

import org.jspecify.annotations.Nullable;

public final class WeatherState {
	public float rainAmount;
	public PrecipitationType precipitationType = PrecipitationType.NORMAL;
	public float windSpeed;
	public boolean heatwave;
	public @Nullable StormState sandstorm;
	public @Nullable StormState snowstorm;

	public void serialize(FriendlyByteBuf buffer) {
		buffer.writeFloat(rainAmount);
		buffer.writeByte(precipitationType.ordinal() & 0xFF);
		buffer.writeFloat(windSpeed);
		buffer.writeBoolean(heatwave);
		buffer.writeBoolean(sandstorm != null);
		if (sandstorm != null) {
			sandstorm.encode(buffer);
		}
		buffer.writeBoolean(snowstorm != null);
		if (snowstorm != null) {
			snowstorm.encode(buffer);
		}
	}

	public void deserialize(FriendlyByteBuf buffer) {
		rainAmount = buffer.readFloat();
		precipitationType = PrecipitationType.VALUES[buffer.readUnsignedByte()];
		windSpeed = buffer.readFloat();
		heatwave = buffer.readBoolean();
		sandstorm = buffer.readBoolean() ? StormState.decode(buffer) : null;
		snowstorm = buffer.readBoolean() ? StormState.decode(buffer) : null;
	}

	public boolean isRaining() {
		return rainAmount > 0.01F;
	}

	public boolean isWindy() {
		return windSpeed > 0.01F;
	}

	public boolean hasWeather() {
		return isRaining() || isWindy()
				|| heatwave
				|| sandstorm != null || snowstorm != null;
	}
}
