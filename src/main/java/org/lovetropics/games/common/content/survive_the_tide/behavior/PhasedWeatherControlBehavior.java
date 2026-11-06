package org.lovetropics.games.common.content.survive_the_tide.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.util.random.WeightedList;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.state.progress.ProgressChannel;
import org.lovetropics.games.common.core.game.state.progress.ProgressHolder;
import org.lovetropics.games.common.core.game.state.progress.ProgressionPeriod;
import org.lovetropics.games.common.core.game.state.weather.GameWeatherState;
import org.lovetropics.games.common.core.game.weather.WeatherEvent;
import org.lovetropics.games.common.core.game.weather.WeatherEventType;
import org.lovetropics.games.common.util.Codecs;

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
	public void register(IGamePhase game, EventRegistrar events) {
		GameWeatherState weather = game.state().getOrThrow(GameWeatherState.KEY);
		ProgressHolder progression = channel.getOrThrow(game);

		class State {
			private @Nullable Period lastPeriod;
			private @Nullable WeatherEventType weather;
		}
		State state = new State();

		events.listen(GamePhaseEvents.TICK, () -> {
			if (game.ticks() % SharedConstants.TICKS_PER_SECOND == 0) {
				Period period = getCurrentPeriod(progression);
				if (period != state.lastPeriod) {
					state.weather = period != null ? period.weather.getRandomOrThrow(game.random()) : null;
					state.lastPeriod = period;
				}
				tick(weather, state.weather);
			}
		});
	}

	private void tick(GameWeatherState weather, @Nullable WeatherEventType weatherType) {
		if (Objects.equals(weatherType, weather.getEventType())) {
			return;
		}
		if (weatherType != null) {
			weather.setEvent(WeatherEvent.createGeneric(weatherType, Long.MAX_VALUE));
		} else {
			weather.setEvent(null);
		}
	}

	private @Nullable Period getCurrentPeriod(ProgressHolder progression) {
		for (Period period : periods) {
			if (progression.is(period.period())) {
				return period;
			}
		}
		return null;
	}

	public record Period(ProgressionPeriod period, WeightedList<WeatherEventType> weather) {
		public static final Codec<Period> CODEC = RecordCodecBuilder.create(i -> i.group(
				ProgressionPeriod.MAP_CODEC.forGetter(Period::period),
				Codecs.compactNonEmptyWeightedList(WeatherEventType.CODEC).fieldOf("weather").forGetter(Period::weather)
		).apply(i, Period::new));
	}
}
