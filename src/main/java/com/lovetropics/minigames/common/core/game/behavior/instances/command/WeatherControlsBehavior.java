package com.lovetropics.minigames.common.core.game.behavior.instances.command;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.weather.PrecipitationType;
import com.lovetropics.minigames.common.core.game.weather.WeatherController;
import com.lovetropics.minigames.common.core.game.weather.WeatherControllerManager;
import com.mojang.serialization.MapCodec;

import java.util.function.Supplier;

public class WeatherControlsBehavior implements IGameBehavior {
	public static final MapCodec<WeatherControlsBehavior> CODEC = MapCodec.unit(WeatherControlsBehavior::new);

	private WeatherController controller;

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		controller = WeatherControllerManager.forWorld(game.level());

		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) -> {
			commands.registerAdmin("start_heatwave", source -> controller.setHeatwave(true));
			commands.registerAdmin("stop_heatwave", source -> controller.setHeatwave(false));

			commands.registerAdmin("start_rain", source -> controller.setRain(1.0F, PrecipitationType.NORMAL));
			commands.registerAdmin("stop_rain", source -> controller.setRain(0.0F, PrecipitationType.NORMAL));

			commands.registerAdmin("start_acid_rain", source -> controller.setRain(1.0F, PrecipitationType.ACID));
			commands.registerAdmin("stop_acid_rain", source -> controller.setRain(0.0F, PrecipitationType.ACID));

			commands.registerAdmin("start_hail", source -> controller.setRain(1.0F, PrecipitationType.HAIL));
			commands.registerAdmin("stop_hail", source -> controller.setRain(0.0F, PrecipitationType.HAIL));

			commands.registerAdmin("start_wind", source -> controller.setWind(0.5F));
			commands.registerAdmin("stop_wind", source -> controller.setWind(0.0F));
		});

		events.listen(GamePhaseEvents.STOP, reason -> controller.reset());
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.WEATHER_CONTROLS;
	}
}
