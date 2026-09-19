package com.lovetropics.minigames.common.content.survive_the_tide;

import com.lovetropics.minigames.common.core.game.state.progress.DiscreteProgressionMap;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressHolder;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressionPeriod;
import com.lovetropics.minigames.common.core.game.weather.WeatherEventType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;

public class SurviveTheTideWeatherConfig {
	public static final MapCodec<SurviveTheTideWeatherConfig> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.unboundedMap(WeatherEventType.CODEC, DiscreteProgressionMap.codec(Codec.FLOAT)).fieldOf("event_chances").forGetter(c -> c.eventChancesByTime),
			Codec.unboundedMap(WeatherEventType.CODEC, Timers.CODEC).fieldOf("event_timers").forGetter(c -> c.eventTimers),
			ProgressionPeriod.CODEC.fieldOf("halve_event_time").forGetter(c -> c.halveEventTime),
			Codec.INT.optionalFieldOf("sandstorm_buildup_tickrate", 40).forGetter(c -> c.sandstormBuildupTickRate),
			Codec.INT.optionalFieldOf("sandstorm_max_stackable", 1).forGetter(c -> c.sandstormMaxStackable),
			Codec.INT.optionalFieldOf("snowstorm_buildup_tickrate", 40).forGetter(c -> c.snowstormBuildupTickRate),
			Codec.INT.optionalFieldOf("snowstorm_max_stackable", 1).forGetter(c -> c.snowstormMaxStackable)
	).apply(i, SurviveTheTideWeatherConfig::new));

	private final Map<WeatherEventType, DiscreteProgressionMap<Float>> eventChancesByTime;
	private final Map<WeatherEventType, Timers> eventTimers;

	private final ProgressionPeriod halveEventTime;

	private final int sandstormBuildupTickRate;
	private final int sandstormMaxStackable;

	private final int snowstormBuildupTickRate;
	private final int snowstormMaxStackable;

	public SurviveTheTideWeatherConfig(
			Map<WeatherEventType, DiscreteProgressionMap<Float>> eventChancesByTime,
			Map<WeatherEventType, Timers> eventTimers,
			ProgressionPeriod halveEventTime,
			int sandstormBuildupTickRate, int sandstormMaxStackable,
			int snowstormBuildupTickRate, int snowstormMaxStackable
	) {
		this.eventChancesByTime = eventChancesByTime;
		this.eventTimers = eventTimers;
		this.halveEventTime = halveEventTime;

		this.sandstormBuildupTickRate = sandstormBuildupTickRate;
		this.sandstormMaxStackable = sandstormMaxStackable;

		this.snowstormBuildupTickRate = snowstormBuildupTickRate;
		this.snowstormMaxStackable = snowstormMaxStackable;
	}

	private float getEventChance(WeatherEventType event, ProgressHolder progression) {
		DiscreteProgressionMap<Float> chances = eventChancesByTime.get(event);
		if (chances == null) {
			return 0.0f;
		}
		return chances.getOrDefault(progression, 0.0f);
	}

	private Timers getTimers(WeatherEventType event) {
		return eventTimers.getOrDefault(event, Timers.DEFAULT);
	}

	public float getRainHeavyChance(ProgressHolder progression) {
		return getEventChance(WeatherEventType.HEAVY_RAIN, progression);
	}

	public float getRainAcidChance(ProgressHolder progression) {
		return getEventChance(WeatherEventType.ACID_RAIN, progression);
	}

	public float getHailChance(ProgressHolder progression) {
		return getEventChance(WeatherEventType.HAIL, progression);
	}

	public float getHeatwaveChance(ProgressHolder progression) {
		return getEventChance(WeatherEventType.HEATWAVE, progression);
	}

	public float getSandstormChance(ProgressHolder progression) {
		return getEventChance(WeatherEventType.SANDSTORM, progression);
	}

	public float getSnowstormChance(ProgressHolder progression) {
		return getEventChance(WeatherEventType.SNOWSTORM, progression);
	}

	public int getRainHeavyMinTime() {
		return getTimers(WeatherEventType.HEAVY_RAIN).minTime;
	}

	public int getRainHeavyExtraRandTime() {
		return getTimers(WeatherEventType.HEAVY_RAIN).extraRandTime;
	}

	public int getHeatwaveMinTime() {
		return getTimers(WeatherEventType.HEATWAVE).minTime;
	}

	public int getHeatwaveExtraRandTime() {
		return getTimers(WeatherEventType.HEATWAVE).extraRandTime;
	}

	public int getRainAcidMinTime() {
		return getTimers(WeatherEventType.ACID_RAIN).minTime;
	}

	public int getRainAcidExtraRandTime() {
		return getTimers(WeatherEventType.ACID_RAIN).extraRandTime;
	}

	public int getSandstormBuildupTickRate() {
		return sandstormBuildupTickRate;
	}

	public int getSandstormMaxStackable() {
		return sandstormMaxStackable;
	}

	public int getSnowstormBuildupTickRate() {
		return snowstormBuildupTickRate;
	}

	public int getSnowstormMaxStackable() {
		return snowstormMaxStackable;
	}

	// TODO: This is not a good way to do things at all
	public boolean halveEventTime(ProgressHolder progression) {
		return progression.is(halveEventTime);
	}

	public record Timers(int minTime, int extraRandTime) {
		public static final Timers DEFAULT = new Timers(1200, 1200);

		public static final Codec<Timers> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("min_time").forGetter(c -> c.minTime),
				Codec.INT.fieldOf("extra_rand_time").forGetter(c -> c.extraRandTime)
		).apply(i, Timers::new));
	}
}
