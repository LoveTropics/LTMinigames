package com.lovetropics.minigames.common.core.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;

public final class MapWorldInfo extends DerivedLevelData {
	private final MapWorldSettings settings;

	public MapWorldInfo(WorldData serverConfiguration, ServerLevelData overworld) {
		super(serverConfiguration, overworld);
		settings = MapWorldSettings.createFrom(overworld);
	}

	public MapWorldInfo(WorldData serverConfiguration, ServerLevelData overworld, MapWorldSettings settings) {
		super(serverConfiguration, overworld);
		this.settings = settings;
	}

	public static MapWorldInfo create(MinecraftServer server, MapWorldSettings settings) {
		return new MapWorldInfo(server.getWorldData(), (ServerLevelData) server.overworld().getLevelData(), settings);
	}

	@Override
	public void setDayTime(long time) {
		settings.timeOfDay = time;
	}

	@Override
	public long getDayTime() {
		return settings.timeOfDay;
	}

	@Override
	public void setClearWeatherTime(int time) {
		settings.sunnyTime = time;
	}

	@Override
	public void setRaining(boolean raining) {
		settings.raining = raining;
	}

	@Override
	public void setRainTime(int time) {
		settings.rainTime = time;
	}

	@Override
	public void setThundering(boolean thundering) {
		settings.thundering = thundering;
	}

	@Override
	public void setThunderTime(int time) {
		settings.thunderTime = time;
	}

	public void setDifficulty(Difficulty difficulty) {
		settings.difficulty = difficulty;
	}

	@Override
	public boolean isRaining() {
		return settings.raining;
	}

	@Override
	public int getRainTime() {
		return settings.rainTime;
	}

	@Override
	public boolean isThundering() {
		return settings.thundering;
	}

	@Override
	public int getThunderTime() {
		return settings.thunderTime;
	}

	@Override
	public int getClearWeatherTime() {
		return settings.sunnyTime;
	}

	@Override
	public GameRules getGameRules() {
		return settings.gameRules;
	}

	@Override
	public Difficulty getDifficulty() {
		return settings.difficulty;
	}
}
