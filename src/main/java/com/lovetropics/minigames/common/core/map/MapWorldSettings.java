package com.lovetropics.minigames.common.core.map;

import com.lovetropics.minigames.common.hack.GrossHackyGameRuleCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.WeatherData;

public final class MapWorldSettings {

	public static final Codec<MapWorldSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
			new GrossHackyGameRuleCodec().fieldOf("game_rules").forGetter(s -> s.gameRules),
			Codec.LONG.fieldOf("time_of_day").forGetter(s -> s.timeOfDay),
			Codec.INT.fieldOf("sunny_time").forGetter(s -> s.weather.getClearWeatherTime()),
			Codec.BOOL.fieldOf("raining").forGetter(s -> s.weather.isRaining()),
			Codec.INT.fieldOf("rain_time").forGetter(s -> s.weather.getRainTime()),
			Codec.BOOL.fieldOf("thundering").forGetter(s -> s.weather.isThundering()),
			Codec.INT.fieldOf("thunder_time").forGetter(s -> s.weather.getThunderTime()),
			Difficulty.CODEC.fieldOf("difficulty").forGetter(s -> s.difficulty)
	).apply(i, MapWorldSettings::new));

	public GameRules gameRules;
	public long timeOfDay;
	public final WeatherData weather;
	public Difficulty difficulty;

	public MapWorldSettings(GameRules gameRules, long timeOfDay, int sunnyTime, boolean raining, int rainTime, boolean thundering, int thunderTime, Difficulty difficulty) {
		this.gameRules = gameRules;
		this.timeOfDay = timeOfDay;
		this.weather = new WeatherData(sunnyTime, rainTime, thunderTime, raining, thundering);
		this.difficulty = difficulty;
	}

	public MapWorldSettings() {
		this(new GameRules(FeatureFlagSet.of()), 0, 0, false, 0, false, 0, Difficulty.NORMAL);
	}

	public static MapWorldSettings createFromOverworld(MinecraftServer server) {
		WeatherData weather = server.getWeatherData();
		return new MapWorldSettings(
				server.getGameRules().copy(FeatureFlagSet.of()),
				server.overworld().getDefaultClockTime(),
				weather.getClearWeatherTime(),
				weather.isRaining(),
				weather.getRainTime(),
				weather.isThundering(),
				weather.getThunderTime(),
				server.getWorldData().getDifficulty()
		);
	}

	public void importFrom(MapWorldSettings settings) {
		gameRules = settings.gameRules;
		timeOfDay = settings.timeOfDay;
		weather.setClearWeatherTime(settings.weather.getClearWeatherTime());
		weather.setRaining(settings.weather.isRaining());
		weather.setRainTime(settings.weather.getRainTime());
		weather.setThundering(settings.weather.isThundering());
		weather.setThunderTime(settings.weather.getThunderTime());
		difficulty = settings.difficulty;
	}
}
