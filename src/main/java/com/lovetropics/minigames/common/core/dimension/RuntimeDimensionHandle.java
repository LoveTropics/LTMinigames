package com.lovetropics.minigames.common.core.dimension;

import com.google.common.base.Preconditions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.concurrent.atomic.AtomicBoolean;

public final class RuntimeDimensionHandle {
	final RuntimeDimensions dimensions;
	final ServerLevel level;
	final AtomicBoolean deleted = new AtomicBoolean();

	RuntimeDimensionHandle(RuntimeDimensions dimensions, ServerLevel level) {
		this.dimensions = dimensions;
		this.level = level;
	}

	public ResourceKey<Level> asKey() {
		return level.dimension();
	}

	public ServerLevel asLevel() {
		Preconditions.checkState(!deleted.get(), "dimension is queued for deletion!");
		return level;
	}

	public void delete() {
		if (deleted.compareAndSet(false, true)) {
			dimensions.enqueueDeletion(level);
		}
	}
}
