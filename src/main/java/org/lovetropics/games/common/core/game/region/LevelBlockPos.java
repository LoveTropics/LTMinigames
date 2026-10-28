package org.lovetropics.games.common.core.game.region;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public record LevelBlockPos(
		ServerLevel level,
		BlockPos pos
) {
}
