package org.lovetropics.games.common.core.game.behavior.instances.command;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.weather.PrecipitationType;
import org.lovetropics.games.common.core.game.weather.WeatherController;
import org.lovetropics.games.common.core.game.weather.WeatherControllerManager;

import java.util.List;

public class WeatherControlsBehavior implements IGameBehavior {
	public static final MapCodec<WeatherControlsBehavior> CODEC = MapCodec.unit(WeatherControlsBehavior::new);

	private WeatherController controller;

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		List<ServerLevel> levels = game.allLevels();
		if (levels.size() != 1) {
			throw new GameException(Component.literal("Weather only supports 1 dimension"));
		}
		controller = WeatherControllerManager.forWorld(levels.getFirst());

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
	public MapCodec<? extends IGameBehavior> codec() {
		return CODEC;
	}
}
