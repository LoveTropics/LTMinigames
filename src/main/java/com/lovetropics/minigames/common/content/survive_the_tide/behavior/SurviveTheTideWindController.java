package com.lovetropics.minigames.common.content.survive_the_tide.behavior;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.state.progress.DiscreteProgressionMap;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressChannel;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressHolder;
import com.lovetropics.minigames.common.core.game.state.weather.GameWeatherState;
import com.lovetropics.minigames.common.core.game.weather.WeatherEventType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.server.level.ServerLevel;

import org.jspecify.annotations.Nullable;

public class SurviveTheTideWindController implements IGameBehavior {
	public static final MapCodec<SurviveTheTideWindController> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			DiscreteProgressionMap.codec(Codec.FLOAT).fieldOf("wind_speed").forGetter(c -> c.windSpeedByTime)
	).apply(i, SurviveTheTideWindController::new));

	private final DiscreteProgressionMap<Float> windSpeedByTime;

	protected @Nullable ProgressHolder progression;
	protected GameWeatherState weather;

	public SurviveTheTideWindController(DiscreteProgressionMap<Float> windSpeedByTime) {
		this.windSpeedByTime = windSpeedByTime;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		weather = game.state().getOrThrow(GameWeatherState.KEY);
		progression = ProgressChannel.MAIN.getOrThrow(game);
		events.listen(GamePhaseEvents.TICK, () -> tick(game));
	}

	private void tick(final IGamePhase game) {
		if (progression == null) {
			return;
		}

		ServerLevel level = game.level();
		if (level.getGameTime() % SharedConstants.TICKS_PER_SECOND == 0) {
			if (weather.getEventType() == WeatherEventType.SNOWSTORM || weather.getEventType() == WeatherEventType.SANDSTORM) {
				weather.setWind(0.7F);
			} else {
				weather.setWind(windSpeedByTime.getOrDefault(progression, 0.0f));
			}
		}
	}
}
