package org.lovetropics.games.common.content.biodiversity_blitz.entity.ai;

import org.lovetropics.games.common.content.biodiversity_blitz.entity.PlotWalls;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public final class BbMobBrain {
	private static final int SCARE_RADIUS_2 = 2 * 2;

	private final List<BlockPos> scarySources = new ArrayList<>();
	private final PlotWalls plotWalls;

	public BbMobBrain(PlotWalls plotWalls) {
		this.plotWalls = plotWalls;
	}

	public void addScarySource(BlockPos pos) {
		if (!scarySources.contains(pos)) {
			scarySources.add(pos);
		}
	}

	public boolean isScaredAt(int x, int y, int z) {
		for (BlockPos source : scarySources) {
			int dx = source.getX() - x;
			int dy = source.getY() - y;
			int dz = source.getZ() - z;
			int distance2 = dx * dx + dy * dy + dz * dz;
			if (distance2 <= SCARE_RADIUS_2) {
				return true;
			}
		}
		return false;
	}

	public PlotWalls getPlotWalls() {
		return plotWalls;
	}
}
