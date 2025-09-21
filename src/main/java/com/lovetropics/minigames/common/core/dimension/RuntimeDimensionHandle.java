package com.lovetropics.minigames.common.core.dimension;

import com.google.common.base.Preconditions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.concurrent.atomic.AtomicBoolean;

public final class RuntimeDimensionHandle {
	final RuntimeDimensions dimensions;
	final ServerLevel world;
	final AtomicBoolean deleted = new AtomicBoolean();

	RuntimeDimensionHandle(RuntimeDimensions dimensions, ServerLevel world) {
		this.dimensions = dimensions;
		this.world = world;
	}

	public ResourceKey<Level> asKey() {
		return world.dimension();
	}

	public ServerLevel asWorld() {
		Preconditions.checkState(!deleted.get(), "dimension is queued for deletion!");
		return world;
	}

	public void delete() {
		if (deleted.compareAndSet(false, true)) {
			dimensions.enqueueDeletion(world);
		}
	}
}
