package com.lovetropics.minigames.common.content.block;

import com.lovetropics.minigames.common.content.block.TrashBlock.Attachment;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Locale;

public enum TrashType implements NonNullSupplier<Block>, StringRepresentable {

	CAN(6, 7),
	CHIP_BAG(14, 1),
	COLA_STANDING(5, 9),
	COLA(9, 5),
	PLASTIC_BAG(16, 1),
	PLASTIC_BOTTLE(12, 6),
	PLASTIC_RINGS(16, 1),
	STRAW(8, 1),
	;

	private final int w, h;
	private final VoxelShape[] shape = new VoxelShape[6];

	public static final StringRepresentable.EnumCodec<TrashType> CODEC = StringRepresentable.fromEnum(TrashType::values);

	TrashType() {
		this(7);
	}

	TrashType(int w) {
		this(w, 15);
	}

	TrashType(int w, int h) {
		this.w = w;
		this.h = h;
		float halfW = w / 2f;
		for (int i = 0; i < 6; i++) {
			Direction dir = Direction.from3DDataValue(i);
			float min = 8 - halfW;
			float max = 8 + halfW;
			final float minX, maxX, minY, maxY, minZ, maxZ;
			if (dir.getAxis().isVertical()) {
				minX = minZ = min;
				maxX = maxZ = max;
				if (dir == Direction.DOWN) {
					minY = 0;
					maxY = h;
				} else {
					minY = 16 - h;
					maxY = 16;
				}
			} else {
				minY = min;
				maxY = max;
				if (dir.getAxis() == Axis.X) {
					minZ = minY;
					maxZ = maxY;
					if (dir.getAxisDirection() == AxisDirection.POSITIVE) {
						maxX = 16;
						minX = 16 - h;
					} else {
						minX = 0;
						maxX = h;
					}
				} else {
					minX = minY;
					maxX = maxY;
					if (dir.getAxisDirection() == AxisDirection.POSITIVE) {
						maxZ = 16;
						minZ = 16 - h;
					} else {
						minZ = 0;
						maxZ = h;
					}
				}
			}
			shape[i] = Block.box(minX, minY, minZ, maxX, maxY, maxZ);
		}
	}

	public int getModelYOffset() {
		return Math.max(16 - h - 4, 0) / 2;
	}

	public float getModelScale(float base) {
		return ((16 - Math.max(w, h)) / 16f) * base + base;
	}

	public VoxelShape getShape(Direction facing, Attachment attachment) {
		return getShape(attachment == Attachment.WALL ? facing : attachment == Attachment.FLOOR ? Direction.DOWN : Direction.UP);
	}

	private VoxelShape getShape(Direction dir) {
		return shape[dir.get3DDataValue()];
	}

	public String getId() {
		return name().toLowerCase(Locale.ROOT);
	}

	@Override
	public String getSerializedName() {
		return getId();
	}

	@Override
	public Block get() {
		return LoveTropicsBlocks.TRASH.get(this).get();
	}
}
