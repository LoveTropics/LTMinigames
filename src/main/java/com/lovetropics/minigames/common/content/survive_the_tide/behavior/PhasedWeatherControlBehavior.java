package com.lovetropics.minigames.common.content.survive_the_tide.behavior;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressChannel;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressHolder;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressionPeriod;
import com.lovetropics.minigames.common.core.game.state.weather.GameWeatherState;
import com.lovetropics.minigames.common.core.game.weather.WeatherEvent;
import com.lovetropics.minigames.common.core.game.weather.WeatherEventType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.server.level.ServerLevel;

import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.Objects;

public record PhasedWeatherControlBehavior(
		ProgressChannel channel,
		List<Period> periods
) implements IGameBehavior {
	public static final MapCodec<PhasedWeatherControlBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ProgressChannel.CODEC.optionalFieldOf("channel", ProgressChannel.MAIN).forGetter(PhasedWeatherControlBehavior::channel),
			Period.CODEC.listOf().fieldOf("periods").forGetter(PhasedWeatherControlBehavior::periods)
	).apply(i, PhasedWeatherControlBehavior::new));

	@Override
	public void register(final IGamePhase game, final EventRegistrar events) {
		final GameWeatherState weather = game.state().getOrThrow(GameWeatherState.KEY);
		final ProgressHolder progression = channel.getOrThrow(game);

		events.listen(GamePhaseEvents.TICK, () -> {
			final ServerLevel level = game.level();
			if (level.getGameTime() % SharedConstants.TICKS_PER_SECOND == 0) {
				tick(weather, progression);
			}
		});
	}

	private void tick(final GameWeatherState weather, final ProgressHolder progression) {
		final WeatherEventType weatherType = getCurrentWeather(progression);
		if (Objects.equals(weatherType, weather.getEventType())) {
			return;
		}
		if (weatherType != null) {
			weather.setEvent(WeatherEvent.createGeneric(weatherType, Long.MAX_VALUE));
		} else {
			weather.setEvent(null);
		}
	}

	private @Nullable WeatherEventType getCurrentWeather(final ProgressHolder progression) {
		for (final Period period : periods) {
			if (progression.is(period.period())) {
				return period.weather();
			}
		}
		return null;
	}

	public record Period(ProgressionPeriod period, WeatherEventType weather) {
		public static final Codec<Period> CODEC = RecordCodecBuilder.create(i -> i.group(
				ProgressionPeriod.MAP_CODEC.forGetter(Period::period),
				WeatherEventType.CODEC.fieldOf("weather").forGetter(Period::weather)
		).apply(i, Period::new));
	}
}
