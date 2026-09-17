package com.lovetropics.minigames.common.core.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import org.jspecify.annotations.Nullable;

public final class MapWorldInfo extends DerivedLevelData {
	private final MapWorldSettings settings;

	private @Nullable ServerLevel level;
	private @Nullable ServerClockManager clockManager;
	private boolean advanceTime;

	public MapWorldInfo(WorldData serverConfiguration, ServerLevelData overworld, MapWorldSettings settings) {
		super(serverConfiguration, overworld);
		this.settings = settings;
	}

	public static MapWorldInfo create(MinecraftServer server, MapWorldSettings settings) {
		return new MapWorldInfo(server.getWorldData(), (ServerLevelData) server.overworld().getLevelData(), settings);
	}

	/// Map levels run their own set of world clocks, so that their time is never shared with any other level
	public ServerClockManager getOrCreateClockManager(ServerLevel level) {
		if (clockManager != null) {
			return clockManager;
		}

		ServerClockManager clocks = ServerClockManager.TYPE.factory().create(level);
		clocks.init(level.getServer());
		((MapClocks.Access) clocks).ltminigames$setLevel(level);

		this.level = level;
		clockManager = clocks;
		advanceTime = level.getGameRules().get(GameRules.ADVANCE_TIME);
		level.dimensionType().defaultClock().ifPresent(clock -> clocks.setTotalTicks(clock, settings.timeOfDay));

		return clocks;
	}

	public void tickClocks() {
		ServerLevel level = this.level;
		ServerClockManager clocks = clockManager;
		if (level == null || clocks == null) {
			return;
		}

		clocks.tick();

		boolean advanceTime = level.getGameRules().get(GameRules.ADVANCE_TIME);
		if (advanceTime != this.advanceTime) {
			this.advanceTime = advanceTime;
			// Clients keep predicting the clocks from their last known rate, so they need to hear when time stops or starts
			MapClocks.broadcast(level.getServer().getPlayerList(), clocks, clocks.createFullSyncPacket());
		}

		level.dimensionType().defaultClock().ifPresent(clock -> settings.timeOfDay = clocks.getTotalTicks(clock));
	}

	public void importFrom(MapWorldSettings settings) {
		this.settings.importFrom(settings);

		ServerLevel level = this.level;
		ServerClockManager clocks = clockManager;
		if (level != null && clocks != null) {
			level.dimensionType().defaultClock().ifPresent(clock -> clocks.setTotalTicks(clock, settings.timeOfDay));
		}
	}

	public WeatherData getWeatherData() {
		return settings.weather;
	}

	public GameRules getGameRules() {
		return settings.gameRules;
	}

	public void setDifficulty(Difficulty difficulty) {
		settings.difficulty = difficulty;
	}

	@Override
	public Difficulty getDifficulty() {
		return settings.difficulty;
	}
}
