package org.lovetropics.games.common.content.biodiversity_blitz.entity.ai;

import org.lovetropics.games.common.content.biodiversity_blitz.entity.BbMobEntity;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class BbGroundNavigator extends GroundPathNavigation {
	// Increase how far we can search for a path - should be ok for performance, as we're limited to 2D anyway
	private static final int MAX_DEPTH = 1024;

	private static final float MAX_DISTANCE_TO_WAYPOINT = 0.2f;
	private final BbMobEntity mob;

	public BbGroundNavigator(Mob mob) {
		super(mob, mob.level());
		this.mob = (BbMobEntity) mob;
	}

	@Override
	protected PathFinder createPathFinder(int maxDepth) {
		nodeEvaluator = new NodeProcessor();
		nodeEvaluator.setCanPassDoors(true);
		return new PathFinder(nodeEvaluator, MAX_DEPTH);
	}

	@Override
	public boolean canCutCorner(PathType type) {
		return false;
	}

	@Override
	protected void followThePath() {
		if (!mob.navigateBlockGrid()) {
			super.followThePath();

			return;
		}

		Vec3 pos = getTempMobPos();
		Mob mob = this.mob.asMob();
		maxDistanceToWaypoint = MAX_DISTANCE_TO_WAYPOINT;
		Vec3i nextNodePos = path.getNextNodePos();
		double deltaX = Math.abs(mob.getX() - (nextNodePos.getX() + 0.5));
		double deltaY = Math.abs(mob.getY() - nextNodePos.getY());
		double deltaZ = Math.abs(mob.getZ() - (nextNodePos.getZ() + 0.5));
		if (deltaX <= maxDistanceToWaypoint && deltaZ <= maxDistanceToWaypoint && deltaY < 1.0) {
			path.advance();
		}
		doStuckDetection(pos);
	}

	final class NodeProcessor extends WalkNodeEvaluator {
		@Override
		public PathType getPathTypeOfMob(PathfindingContext context, int x, int y, int z, Mob mob) {
			// Don't allow climbing above the bounds of the plant bounds
			int plotTopY = BbGroundNavigator.this.mob.getPlot().plantBounds.max().getY();
			if (y > plotTopY && this.mob.getY() <= plotTopY) {
				return PathType.BLOCKED;
			}

			return super.getPathTypeOfMob(context, x, y, z, mob);
		}

		@Override
		public PathType getPathType(PathfindingContext context, int x, int y, int z) {
			BbMobBrain brain = BbGroundNavigator.this.mob.getMobBrain();
			if (!brain.getPlotWalls().getBounds().contains(x + 0.5, y + 0.5, z + 0.5)) {
				return PathType.BLOCKED;
			}

			PathType nodeType = super.getPathType(context, x, y, z);
			if (nodeType.getMalus() >= 0.0F && brain.isScaredAt(x, y, z)) {
				return PathType.BLOCKED;
			}

			return nodeType;
		}

		@Override
		protected boolean isDiagonalValid(Node root, @Nullable Node xNode, @Nullable Node zNode) {
			return false;
		}
	}
}
