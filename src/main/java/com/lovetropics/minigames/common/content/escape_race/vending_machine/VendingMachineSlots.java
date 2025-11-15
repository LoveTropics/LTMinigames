package com.lovetropics.minigames.common.content.escape_race.vending_machine;

import com.google.common.collect.ImmutableList;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

public class VendingMachineSlots {
	private static final int ROWS = 4;
	private static final int COLUMNS = 4;

	public static final int COUNT = ROWS * COLUMNS;

	private static final float PICK_RANGE = 5.0f;

	public static final List<VendingMachineSlot> SLOTS = Util.make(() -> {
		ImmutableList.Builder<VendingMachineSlot> slots = ImmutableList.builderWithExpectedSize(COUNT);
		float leftX = -9.0f / 16.0f;
		float topY = -9.0f / 16.0f;
		float spacingX = 5.0f / 16.0f;
		float spacingY = 6.0f / 16.0f;
		float z = -3.0f / 16.0f;
		for (int row = 0; row < ROWS; row++) {
			for (int column = 0; column < COLUMNS; column++) {
				slots.add(new VendingMachineSlot(new Vec3(leftX + column * spacingX, topY + row * spacingY, z), 0.2f));
			}
		}
		return slots.build();
	});

	public static Picker picker(Camera camera, VendingMachineEntity vendingMachine) {
		Vector3f lookVector = camera.getLookVector();

		Vec3 fromPos = VendingMachineModel.toModelSpace(
				camera.position(),
				vendingMachine.position(),
				vendingMachine.getYRot()
		);
		Vec3 toPos = VendingMachineModel.toModelSpace(
				camera.position().add(lookVector.x * PICK_RANGE, lookVector.y * PICK_RANGE, lookVector.z * PICK_RANGE),
				vendingMachine.position(),
				vendingMachine.getYRot()
		);

		return new Picker(fromPos, toPos);
	}

	public record Picker(
			Vec3 from,
			Vec3 to
	) {
		public int pickSlot() {
			for (int i = 0; i < SLOTS.size(); i++) {
				if (isPicked(SLOTS.get(i).bounds())) {
					return i;
				}
			}
			return VendingMachineEntity.NO_SLOT;
		}

		public boolean isPicked(AABB bounds) {
			return bounds.clip(from, to).isPresent();
		}
	}
}
