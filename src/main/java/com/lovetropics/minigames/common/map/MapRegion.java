package com.lovetropics.minigames.common.map;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Iterator;

public final class MapRegion implements Iterable<BlockPos> {
	public final BlockPos min;
	public final BlockPos max;

	private MapRegion(BlockPos min, BlockPos max) {
		this.min = min;
		this.max = max;
	}

	public static MapRegion of(BlockPos pos) {
		return new MapRegion(pos, pos);
	}

	public static MapRegion of(BlockPos a, BlockPos b) {
		return new MapRegion(MapRegion.min(a, b), MapRegion.max(a, b));
	}

	public static BlockPos min(BlockPos a, BlockPos b) {
		return new BlockPos(
				Math.min(a.getX(), b.getX()),
				Math.min(a.getY(), b.getY()),
				Math.min(a.getZ(), b.getZ())
		);
	}

	public static BlockPos max(BlockPos a, BlockPos b) {
		return new BlockPos(
				Math.max(a.getX(), b.getX()),
				Math.max(a.getY(), b.getY()),
				Math.max(a.getZ(), b.getZ())
		);
	}

	public Vec3d getCenter() {
		return new Vec3d(
				(min.getX() + max.getX() + 1.0) / 2.0,
				(min.getY() + max.getY() + 1.0) / 2.0,
				(min.getZ() + max.getZ() + 1.0) / 2.0
		);
	}

	public boolean contains(BlockPos pos) {
		return contains(pos.getX(), pos.getY(), pos.getZ());
	}

	public boolean contains(Vec3d pos) {
		return contains(pos.x, pos.y, pos.z);
	}

	public boolean contains(double x, double y, double z) {
		return x >= min.getX() && y >= min.getY() && z >= min.getZ() && x <= max.getX() && y <= max.getY() && z <= max.getZ();
	}

	public boolean contains(int x, int y, int z) {
		return x >= min.getX() && y >= min.getY() && z >= min.getZ() && x <= max.getX() && y <= max.getY() && z <= max.getZ();
	}

	@Override
	public Iterator<BlockPos> iterator() {
		return BlockPos.getAllInBoxMutable(min, max).iterator();
	}

	public CompoundNBT write(CompoundNBT root) {
		root.put("min", writeBlockPos(min, new CompoundNBT()));
		root.put("max", writeBlockPos(min, new CompoundNBT()));
		return root;
	}

	public static MapRegion read(CompoundNBT root) {
		BlockPos min = readBlockPos(root);
		BlockPos max = readBlockPos(root);
		return new MapRegion(min, max);
	}

	public void write(PacketBuffer buffer) {
		buffer.writeBlockPos(min);
		buffer.writeBlockPos(max);
	}

	public static MapRegion read(PacketBuffer buffer) {
		BlockPos min = buffer.readBlockPos();
		BlockPos max = buffer.readBlockPos();
		return new MapRegion(min, max);
	}

	private static CompoundNBT writeBlockPos(BlockPos pos, CompoundNBT root) {
		root.putInt("x", pos.getX());
		root.putInt("y", pos.getY());
		root.putInt("z", pos.getZ());
		return root;
	}

	private static BlockPos readBlockPos(CompoundNBT root) {
		return new BlockPos(root.getInt("x"), root.getInt("y"), root.getInt("z"));
	}
}
