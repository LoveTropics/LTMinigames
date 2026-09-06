package com.lovetropics.minigames.common.content.survive_the_tide.behavior;

import com.lovetropics.minigames.common.content.survive_the_tide.SurviveTheTideWeatherConfig;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressChannel;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressHolder;
import com.lovetropics.minigames.common.core.game.state.weather.GameWeatherState;
import com.lovetropics.minigames.common.core.game.weather.WeatherEvent;
import com.mojang.serialization.MapCodec;
import net.minecraft.SharedConstants;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

import org.jspecify.annotations.Nullable;

public class SurviveTheTideWeatherControlBehavior implements IGameBehavior {
	public static final MapCodec<SurviveTheTideWeatherControlBehavior> CODEC = SurviveTheTideWeatherConfig.CODEC.xmap(SurviveTheTideWeatherControlBehavior::new, b -> b.config);

	private final SurviveTheTideWeatherConfig config;

	private final RandomSource random = RandomSource.create();

	/*
	 * instantiate in IslandRoyaleMinigameDefinition
	 * - packet sync what is needed
	 * - setup instanced overrides on client
	 * <p>
	 * <p>
	 * phases:
	 * - 1: semi peacefull, maybe light rain/wind
	 * - 2: heavy wind, acid rain
	 * - 3: see doc, "an extreme storm encroaches the map slowly towards the centre"
	 * --- assuming can also do same things phase 2 does?
	 * <p>
	 * phases should be in IslandRoyaleMinigameDefinition for use in other places, and this class listens to them
	 * <p>
	 * rng that can happen:
	 * - wind, can operate independently of other rng events
	 * <p>
	 * rng that only allows 1 of them at a time:
	 * - extreme rain
	 * - acid rain
	 * - heat wave
	 * <p>
	 * heat wave:
	 * - player movement reduced if player pos can see sky
	 * <p>
	 * rain:
	 * - the usual
	 * <p>
	 * acid rain:
	 * - player damage over time
	 * - degrade items and armor over time
	 * - use normal rain visual too, color changed
	 * <p>
	 * extreme rain:
	 * - fog closes in
	 * - pump up weather2 effects
	 * - splashing noise while walking
	 * - use normal rain visual too
	 * <p>
	 * - consider design to factor in worn items to negate player effects
	 */

	protected @Nullable ProgressHolder progression;
	protected GameWeatherState weather;

	public SurviveTheTideWeatherControlBehavior(final SurviveTheTideWeatherConfig config) {
		this.config = config;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		weather = game.state().getOrThrow(GameWeatherState.KEY);

		events.listen(GamePhaseEvents.TICK, () -> tick(game));

		progression = ProgressChannel.MAIN.getOrThrow(game);
	}

	private void tick(final IGamePhase game) {
		if (progression == null) {
			return;
		}

		ServerLevel world = game.level();
		if (world.getGameTime() % SharedConstants.TICKS_PER_SECOND == 0) {
			if (weather.getEvent() == null && weather.canStartWeatherEvent()) {
				if (random.nextFloat() <= config.getRainHeavyChance(progression)) {
					heavyRainfallStart(progression);
				} else if (random.nextFloat() <= config.getRainAcidChance(progression)) {
					acidRainStart(progression);
				} else if (random.nextFloat() <= config.getHailChance(progression)) {
					hailStart(progression);
				} else if (random.nextFloat() <= config.getHeatwaveChance(progression)) {
					heatwaveStart(progression);
				} else if (random.nextFloat() <= config.getSandstormChance(progression)) {
					sandstormStart(progression);
				} else if (random.nextFloat() <= config.getSnowstormChance(progression)) {
					snowstormStart(progression);
				}
			}
		}
	}

	// TODO phase names
	private void heavyRainfallStart(ProgressHolder progression) {
		int time = config.getRainHeavyMinTime() + random.nextInt(config.getRainHeavyExtraRandTime());
		if (config.halveEventTime(progression)) {
			time /= 2;
		}
		weather.setEvent(WeatherEvent.heavyRain(time));
	}

	private void acidRainStart(ProgressHolder progression) {
		int time = config.getRainAcidMinTime() + random.nextInt(config.getRainAcidExtraRandTime());
		if (config.halveEventTime(progression)) {
			time /= 2;
		}
		weather.setEvent(WeatherEvent.acidRain(time));
	}

	private void hailStart(ProgressHolder progression) {
		int time = config.getRainHeavyMinTime() + random.nextInt(config.getRainHeavyExtraRandTime());
		if (config.halveEventTime(progression)) {
			time /= 2;
		}
		weather.setEvent(WeatherEvent.hail(time));
	}

	private void heatwaveStart(ProgressHolder progression) {
		int time = config.getHeatwaveMinTime() + random.nextInt(config.getHeatwaveExtraRandTime());
		if (config.halveEventTime(progression)) {
			time /= 2;
		}
		weather.setEvent(WeatherEvent.heatwave(time));
	}

	private void sandstormStart(ProgressHolder progression) {
		//TODO: more config
		int time = config.getHeatwaveMinTime() + random.nextInt(config.getHeatwaveExtraRandTime());
		if (config.halveEventTime(progression)) {
			time /= 2;
		}
		weather.setEvent(WeatherEvent.sandstorm(time, config.getSandstormBuildupTickRate(), config.getSandstormMaxStackable()));
	}

	private void snowstormStart(ProgressHolder progression) {
		//TODO: more config
		int time = config.getHeatwaveMinTime() + random.nextInt(config.getHeatwaveExtraRandTime());
		if (config.halveEventTime(progression)) {
			time /= 2;
		}
		weather.setEvent(WeatherEvent.snowstorm(time, config.getSnowstormBuildupTickRate(), config.getSnowstormMaxStackable()));
	}
}
