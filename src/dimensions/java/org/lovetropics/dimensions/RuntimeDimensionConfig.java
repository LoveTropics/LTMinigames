package org.lovetropics.dimensions;

import net.minecraft.world.level.dimension.LevelStem;
import org.jspecify.annotations.Nullable;

public record RuntimeDimensionConfig(
		LevelStem levelStem,
		@Nullable SharedDimensionState sharedState
) {
}
