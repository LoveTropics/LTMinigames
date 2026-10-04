package org.lovetropics.dimensions;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.Util;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;

/* package-private */ class RuntimeServerLevel extends ServerLevel {
	private static final Logger LOGGER = LogUtils.getLogger();

	private final RuntimeDimensionHandle handle;
	private final boolean temporary;

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
				config.worldInfo(),
				dimension,
				config.levelStem(),
				false,
				BiomeManager.obfuscateSeed(server.getWorldGenSettings().options().seed()),
				List.of(),
				false
		);
		handle = new RuntimeDimensionHandle(this);
		this.temporary = temporary;
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
}
