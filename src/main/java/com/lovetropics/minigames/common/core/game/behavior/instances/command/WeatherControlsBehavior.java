package com.lovetropics.minigames.common.core.game.behavior.instances.command;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.state.control.ControlCommand;
import com.lovetropics.minigames.common.core.game.state.control.ControlCommands;
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

		ControlCommands commands = game.controlCommands();
		commands.add("start_heatwave", ControlCommand.forAdmins(source -> controller.setHeatwave(true)));
		commands.add("stop_heatwave", ControlCommand.forAdmins(source -> controller.setHeatwave(false)));

		commands.add("start_rain", ControlCommand.forAdmins(source -> controller.setRain(1.0F, PrecipitationType.NORMAL)));
		commands.add("stop_rain", ControlCommand.forAdmins(source -> controller.setRain(0.0F, PrecipitationType.NORMAL)));

		commands.add("start_acid_rain", ControlCommand.forAdmins(source -> controller.setRain(1.0F, PrecipitationType.ACID)));
		commands.add("stop_acid_rain", ControlCommand.forAdmins(source -> controller.setRain(0.0F, PrecipitationType.ACID)));

		commands.add("start_hail", ControlCommand.forAdmins(source -> controller.setRain(1.0F, PrecipitationType.HAIL)));
		commands.add("stop_hail", ControlCommand.forAdmins(source -> controller.setRain(0.0F, PrecipitationType.HAIL)));

		commands.add("start_wind", ControlCommand.forAdmins(source -> controller.setWind(0.5F)));
		commands.add("stop_wind", ControlCommand.forAdmins(source -> controller.setWind(0.0F)));

		events.listen(GamePhaseEvents.STOP, reason -> controller.reset());
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.WEATHER_CONTROLS;
	}
}
