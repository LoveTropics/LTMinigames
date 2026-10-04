package org.lovetropics.dimensions;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.WeatherData;
import org.lovetropics.dimensions.duck.SharedDimensionStateBindable;

public final class SharedDimensionState {
	private final GameRules gameRules;
	private final ServerClockManager clockManager;
	private final WeatherData weather;
	private Difficulty difficulty = Difficulty.NORMAL;

	private SharedDimensionState(GameRules gameRules, ServerClockManager clockManager, WeatherData weather) {
		this.gameRules = gameRules;
		this.clockManager = clockManager;
		this.weather = weather;
	}

	public static SharedDimensionState createFresh(MinecraftServer server) {
		GameRules gameRules = new GameRules(FeatureFlags.VANILLA_SET);
		ServerClockManager clocks = ServerClockManager.TYPE.factory().create(null);
		clocks.init(server);
		WeatherData weather = new WeatherData();
		SharedDimensionState sharedState = new SharedDimensionState(gameRules, clocks, weather);
		((SharedDimensionStateBindable) clocks).ltdimensions$bindTo(sharedState);
		return sharedState;
	}

	public GameRules gameRules() {
		return gameRules;
	}

	public ServerClockManager clockManager() {
		return clockManager;
	}

	public WeatherData weather() {
		return weather;
	}

	public Difficulty difficulty() {
		return difficulty;
	}

	public void setDifficulty(Difficulty difficulty) {
		this.difficulty = difficulty;
	}
}
