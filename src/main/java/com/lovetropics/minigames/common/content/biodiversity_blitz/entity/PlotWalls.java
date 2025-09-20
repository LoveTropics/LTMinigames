package com.lovetropics.minigames.common.content.biodiversity_blitz.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public final class PlotWalls {
	private final AABB bounds;
	private final AABB[] faces;
	private final VoxelShape[] faceShapes;

	public PlotWalls(AABB bounds) {
		this.bounds = bounds;

		Direction[] directions = Direction.values();
		faces = new AABB[directions.length];
		faceShapes = new VoxelShape[directions.length];

		for (Direction direction : directions) {
			AABB plotFace = createWallBounds(bounds, direction);

			int index = direction.get3DDataValue();
			faces[index] = plotFace;
			faceShapes[index] = Shapes.create(plotFace);
		}
	}

	private AABB createWallBounds(AABB bounds, Direction direction) {
		Direction.Axis axis = direction.getAxis();
		double size = bounds.max(axis) - bounds.min(axis);

		// offset to the edge in this direction
		Vec3 offset = direction.getUnitVec3().scale(size);

		// grow on every other axis to not create any holes
		Vec3 grow = new Vec3(
				axis != Direction.Axis.X ? bounds.getXsize() : 0.0,
				axis != Direction.Axis.Y ? bounds.getYsize() : 0.0,
				axis != Direction.Axis.Z ? bounds.getZsize() : 0.0
		);

		return bounds
				.move(offset)
				.inflate(grow.x, grow.y, grow.z);
	}

	public Vec3 collide(AABB box, Vec3 offset) {
		if (offset.lengthSqr() == 0.0) {
			return offset;
		}

		AABB collidingBox = box.expandTowards(offset);

		// we're definitely not going to collide
		if (!collidingBox.intersects(bounds)) {
			return offset;
		}

		List<VoxelShape> collisions = collectCollisions(collidingBox);
		return Entity.collideWithShapes(offset, box, collisions);
	}

	public List<VoxelShape> collectCollisions(AABB box) {
		final List<VoxelShape> collisions = new ArrayList<>();
		for (int i = 0; i < faces.length; i++) {
			if (box.intersects(faces[i])) {
				collisions.add(faceShapes[i]);
			}
		}
		return collisions;
	}

	public AABB getBounds() {
		return bounds;
	}

	public boolean containsEntity(Entity entity) {
		return bounds.contains(entity.position());
	}

	public boolean containsBlock(BlockPos pos) {
		return bounds.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
	}
}
