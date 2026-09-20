package org.lovetropics.games.common.core.dimension;

import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.ServerLevelData;

public record RuntimeDimensionConfig(LevelStem dimension, long seed, ServerLevelData worldInfo) {
}
