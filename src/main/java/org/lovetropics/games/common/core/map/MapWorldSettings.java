package org.lovetropics.games.common.core.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.WeatherData;
import org.lovetropics.dimensions.SharedDimensionState;

public record MapWorldSettings(
		GameRuleMap gameRules,
		// TODO: Store clocks and weatherdata generally rather than special-casing
		long timeOfDay,
		int sunnyTime,
		boolean raining,
		int rainTime,
		boolean thundering,
		int thunderTime,
		Difficulty difficulty
) {
	// Ignore game rules that may have been added by other mods and removed
	private static final Codec<GameRuleMap> GAME_RULES_CODEC = GameRuleMap.CODEC.promotePartial(_ -> {
	});

	public static final Codec<MapWorldSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
			GAME_RULES_CODEC.fieldOf("game_rules").forGetter(MapWorldSettings::gameRules),
			Codec.LONG.fieldOf("time_of_day").forGetter(MapWorldSettings::timeOfDay),
			Codec.INT.fieldOf("sunny_time").forGetter(MapWorldSettings::sunnyTime),
			Codec.BOOL.fieldOf("raining").forGetter(MapWorldSettings::raining),
			Codec.INT.fieldOf("rain_time").forGetter(MapWorldSettings::rainTime),
			Codec.BOOL.fieldOf("thundering").forGetter(MapWorldSettings::thundering),
			Codec.INT.fieldOf("thunder_time").forGetter(MapWorldSettings::thunderTime),
			Difficulty.CODEC.fieldOf("difficulty").forGetter(MapWorldSettings::difficulty)
	).apply(i, MapWorldSettings::new));

	public void setupInto(MinecraftServer server, SharedDimensionState sharedState) {
		Holder.Reference<WorldClock> overworldClock = server.registryAccess().getOrThrow(WorldClocks.OVERWORLD);
		sharedState.clockManager().setTotalTicks(overworldClock, timeOfDay());

		WeatherData weather = sharedState.weather();
		weather.setClearWeatherTime(sunnyTime);
		weather.setRaining(raining);
		weather.setRainTime(rainTime);
		weather.setThundering(thundering);
		weather.setThunderTime(thunderTime);

		sharedState.setDifficulty(difficulty);
	}

	public static MapWorldSettings copyOf(ServerLevel level) {
		WeatherData weatherData = level.getWeatherData();
		return new MapWorldSettings(
				copyRules(level.getGameRules()),
				level.getDefaultClockTime(),
				weatherData.getClearWeatherTime(),
				weatherData.isRaining(),
				weatherData.getRainTime(),
				weatherData.isThundering(),
				weatherData.getThunderTime(),
				level.getDifficulty()
		);
	}

	private static GameRuleMap copyRules(GameRules source) {
		GameRuleMap.Builder gameRules = new GameRuleMap.Builder();
		source.availableRules().forEach(gameRule ->
				copyRule(gameRule, source, gameRules)
		);
		return gameRules.build();
	}

	private static <T> void copyRule(GameRule<T> gameRule, GameRules source, GameRuleMap.Builder output) {
		output.set(gameRule, source.get(gameRule));
	}
}
