package org.lovetropics.dimensions;

import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ServerLevelData;
import org.jspecify.annotations.Nullable;

public record RuntimeDimensionConfig(
		LevelStem levelStem,
		ServerLevelData worldInfo,
		@Nullable GameRules overrideGameRules
) {
}
