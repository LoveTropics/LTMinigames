package com.lovetropics.minigames.common.content.escape_race.vending_machine;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record VendingMachineSlot(Vec3 pos, float size, AABB bounds) {
	public VendingMachineSlot(Vec3 pos, float size) {
		this(pos, size, AABB.ofSize(pos, size, size, size));
	}
}
