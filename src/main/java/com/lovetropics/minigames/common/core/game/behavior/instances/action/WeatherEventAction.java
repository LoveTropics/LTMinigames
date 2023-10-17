package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.state.weather.GameWeatherState;
import com.lovetropics.minigames.common.core.game.weather.WeatherEvent;
import com.lovetropics.minigames.common.core.game.weather.WeatherEventType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import javax.annotation.Nullable;

public final class WeatherEventAction implements IGameBehavior {
	public static final MapCodec<WeatherEventAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			WeatherEventType.CODEC.fieldOf("event").forGetter(c -> c.type),
			Codec.LONG.fieldOf("seconds").forGetter(c -> c.ticks / 20)
	).apply(i, WeatherEventAction::new));

	public final WeatherEventType type;
	public final long ticks;

	private GameWeatherState weather;

	public WeatherEventAction(WeatherEventType type, long seconds) {
		this.type = type;
		this.ticks = seconds * 20;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		weather = game.getState().getOrThrow(GameWeatherState.KEY);

		events.listen(GameActionEvents.APPLY, (context) -> {
			WeatherEvent event = this.tryCreateEvent(ticks);
			if (event != null) {
				weather.setEvent(event);
				return true;
			} else {
				return false;
			}
		});
	}

	@Nullable
	private WeatherEvent tryCreateEvent(long time) {
		return switch (type) {
			case HEAVY_RAIN -> WeatherEvent.heavyRain(time);
			case ACID_RAIN -> WeatherEvent.acidRain(time);
			case HEATWAVE -> WeatherEvent.heatwave(time);
			default -> null;
		};
	}
}
