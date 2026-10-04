package org.lovetropics.dimensions;

import com.google.common.base.Preconditions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class RuntimeDimensionHandle {
	private final RuntimeServerLevel level;
	private boolean valid = true;

	RuntimeDimensionHandle(RuntimeServerLevel level) {
		this.level = level;
	}

	public ResourceKey<Level> asKey() {
		return level.dimension();
	}

	public ServerLevel asLevel() {
		Preconditions.checkState(isValid(), "Handle is no longer valid");
		return level;
	}

	public void markForDeletion() {
		valid = false;
	}

	public boolean isValid() {
		return valid;
	}

	/* package-private */ void revive() {
		valid = true;
	}
}
