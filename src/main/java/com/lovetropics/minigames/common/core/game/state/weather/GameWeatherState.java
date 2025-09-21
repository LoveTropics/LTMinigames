package com.lovetropics.minigames.common.core.game.state.weather;

import com.lovetropics.minigames.common.core.game.behavior.event.GameWorldEvents;
import com.lovetropics.minigames.common.core.game.state.GameStateKey;
import com.lovetropics.minigames.common.core.game.state.IGameState;
import com.lovetropics.minigames.common.core.game.weather.WeatherController;
import com.lovetropics.minigames.common.core.game.weather.WeatherEvent;
import com.lovetropics.minigames.common.core.game.weather.WeatherEventType;
import net.minecraft.SharedConstants;

import javax.annotation.Nullable;

public final class GameWeatherState implements IGameState {
	public static final GameStateKey<GameWeatherState> KEY = GameStateKey.create("Weather State");

	private final WeatherController controller;
	private final GameWorldEvents.SetWeather weatherListener;

	@Nullable
	private WeatherEvent event;

	private int weatherCooldown = 0;
	private final int weatherCooldownBetweenStates = 11 * SharedConstants.TICKS_PER_SECOND;

	public GameWeatherState(WeatherController controller, GameWorldEvents.SetWeather weatherListener) {
		this.controller = controller;
		this.weatherListener = weatherListener;
	}

	public void clear() {
		clearEvent();
		setWind(0.0F);
	}

	public void tick() {
		if (weatherCooldown > 0) {
			weatherCooldown--;
		}
		WeatherEvent event = this.event;
		if (event != null && event.tick() == WeatherEvent.TickResult.STOP) {
			clearEvent();
			weatherCooldown = weatherCooldownBetweenStates;
		}
	}

	public void setWind(float wind) {
		controller.setWind(wind);
	}

	public void setEvent(@Nullable WeatherEvent event) {
		WeatherEvent lastEvent = this.event;
		if (lastEvent != null) {
			lastEvent.remove(controller);
		}

		this.event = event;
		if (event != null) {
			event.apply(controller);
		}

		weatherListener.onSetWeather(lastEvent, event);
	}

	public void clearEvent() {
		setEvent(null);
	}

	@Nullable
	public WeatherEvent getEvent() {
		return event;
	}

	@Nullable
	public WeatherEventType getEventType() {
		return event != null ? event.getType() : null;
	}

	public boolean canStartWeatherEvent() {
		return weatherCooldown == 0;
	}
}
