package org.lovetropics.dimensions;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.Util;
import net.minecraft.world.Difficulty;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;

@ApiStatus.Internal
public class RuntimeServerLevel extends ServerLevel {
	private static final Logger LOGGER = LogUtils.getLogger();

	private final RuntimeDimensionHandle handle;
	private final boolean temporary;
	private final @Nullable SharedDimensionState sharedState;

	public RuntimeServerLevel(
			MinecraftServer server,
			ResourceKey<Level> dimension,
			RuntimeDimensionConfig config,
			boolean temporary
	) {
		super(
				server,
				Util.backgroundExecutor(),
				server.storageSource,
				new DerivedLevelData(server.getWorldData(), (ServerLevelData) server.overworld().getLevelData()) {
					@Override
					public Difficulty getDifficulty() {
						return config.sharedState() != null ? config.sharedState().difficulty() : super.getDifficulty();
					}
				},
				dimension,
				config.levelStem(),
				false,
				BiomeManager.obfuscateSeed(server.getWorldGenSettings().options().seed()),
				List.of(),
				false
		);
		handle = new RuntimeDimensionHandle(this);
		this.temporary = temporary;
		sharedState = config.sharedState();

		// Matching ServerLevel::prepareWeather, which references MinecraftServer directly
		if (sharedState != null && sharedState.weather().isRaining()) {
			rainLevel = 1.0f;
			if (sharedState.weather().isThundering()) {
				thunderLevel = 1.0f;
			}
		}
	}

	@Override
	public void save(@Nullable ProgressListener progress, boolean flush, boolean skipSave) {
		if (!temporary) {
			super.save(progress, flush, skipSave);
			return;
		}
		try {
			if (!flush && handle.isValid()) {
				super.save(progress, false, skipSave);
			}
		} catch (Exception e) {
			// This is terrible, but if we do encounter an error while saving a temporary dimension - we at least never want to crash the server
			LOGGER.error("Failed to save temporary dimension", e);
		}
	}

	public RuntimeDimensionHandle handle() {
		return handle;
	}

	public boolean isTemporary() {
		return temporary;
	}

	@Override
	public ServerClockManager clockManager() {
		return sharedState != null ? sharedState.clockManager() : super.clockManager();
	}

	@Override
	public GameRules getGameRules() {
		return sharedState != null ? sharedState.gameRules() : super.getGameRules();
	}

	@Override
	public WeatherData getWeatherData() {
		return sharedState != null ? sharedState.weather() : super.getWeatherData();
	}

	public @Nullable SharedDimensionState sharedState() {
		return sharedState;
	}
}
