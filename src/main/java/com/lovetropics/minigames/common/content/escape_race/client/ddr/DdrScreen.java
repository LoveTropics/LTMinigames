package com.lovetropics.minigames.common.content.escape_race.client.ddr;

import com.lovetropics.minigames.common.content.escape_race.client.ddr.render.DDRMachineEntityModel;
import com.lovetropics.minigames.common.content.escape_race.client.ddr.render.DDRMachineEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;

import javax.annotation.Nullable;

public class DdrScreen {
	private static final int MODEL_WIDTH = 42;
	private static final int MODEL_HEIGHT = 28;

	public static final int SCALE = 3;

	public static final int WIDTH = MODEL_WIDTH * SCALE;
	public static final int HEIGHT = MODEL_HEIGHT * SCALE;

	private static final float WORLD_TO_SCREEN_SCALE = 1.0f / 16.0f / SCALE;
	private static final float WORLD_Z_OFFSET = -0.15f / 16.0f;

	public static final int NO_LEVEL_PICKED = -1;

	private final ModelPart screenPart;

	private final PoseStack reusedPoseStack = new PoseStack();

	public DdrScreen(DDRMachineEntityModel model) {
		screenPart = model.getScreen();
	}

	public void applyTransform(PoseStack poseStack, float yRot) {
		DDRMachineEntityRenderer.applyModelTransform(poseStack, yRot);
		screenPart.translateAndRotate(poseStack);
		poseStack.translate(0.0f, 0.0f, WORLD_Z_OFFSET);
		poseStack.scale(DdrScreen.WORLD_TO_SCREEN_SCALE, DdrScreen.WORLD_TO_SCREEN_SCALE, DdrScreen.WORLD_TO_SCREEN_SCALE);
		poseStack.translate(-DdrScreen.WIDTH / 2.0f, -DdrScreen.HEIGHT / 2.0f, 0.0f);
	}

	@Nullable
	public Vector2fc pick(Camera camera, Vec3 entityPos, float entityYRot) {
		reusedPoseStack.setIdentity();
		applyTransform(reusedPoseStack, entityYRot);
		Matrix4fc modelWorldToScreen = reusedPoseStack.last().pose().invert(new Matrix4f());

		Vector3f cameraPos = modelWorldToScreen.transformPosition(new Vector3f(
				(float) (camera.position().x - entityPos.x),
				(float) (camera.position().y - entityPos.y),
				(float) (camera.position().z - entityPos.z)
		));
		if (cameraPos.z() > 0.0f) {
			return null;
		}

		Vector3f cameraVector = modelWorldToScreen.transformDirection(camera.forwardVector(), new Vector3f());
		if (cameraVector.z() < Mth.EPSILON) {
			return null;
		}

		// Scale the camera vector to exactly touch the screen surface
		cameraVector.mul(cameraPos.z() / -cameraVector.z());

		float screenX = cameraPos.x() + cameraVector.x();
		float screenY = cameraPos.y() + cameraVector.y();
		if (screenX < 0.0f || screenY < 0.0f || screenX > WIDTH || screenY > HEIGHT) {
			return null;
		}

		return new Vector2f(screenX, screenY);
	}

	public int pickLevelIndex(Camera camera, Vec3 entityPos, float entityYRot, int levelCount) {
		Vector2fc screenPos = pick(camera, entityPos, entityYRot);
		if (screenPos == null) {
			return NO_LEVEL_PICKED;
		}

		LevelArrangement arrangement = LevelArrangement.forCount(levelCount);
		for (int i = 0; i < levelCount; i++) {
			int left = arrangement.getSlotLeft(i);
			int top = arrangement.getSlotTop(i);
			int right = left + LevelArrangement.SLOT_SIZE_X;
			int bottom = top + LevelArrangement.SLOT_SIZE_Y;
			if (screenPos.x() >= left && screenPos.y() >= top && screenPos.x() <= right && screenPos.y() <= bottom) {
				return i;
			}
		}

		return NO_LEVEL_PICKED;
	}

	public record LevelArrangement(
			int count,
			int width,
			int height
	) {
		public static final int SLOT_SIZE_X = 30;
		public static final int SLOT_SIZE_Y = 20;
		private static final int MAX_PER_ROW = 4;

		public static LevelArrangement forCount(int count) {
			return new LevelArrangement(
					count,
					Math.min(count, MAX_PER_ROW) * SLOT_SIZE_X,
					Mth.positiveCeilDiv(count, MAX_PER_ROW) * SLOT_SIZE_Y
			);
		}

		public int getSlotLeft(int index) {
			return (WIDTH - width) / 2 + (index % MAX_PER_ROW) * SLOT_SIZE_X;
		}

		public int getSlotTop(int index) {
			return (HEIGHT - height) / 2 + (index / MAX_PER_ROW) * SLOT_SIZE_Y;
		}

		public int getCenterX(int index) {
			return getSlotLeft(index) + SLOT_SIZE_X / 2;
		}

		public int getCenterY(int index) {
			return getSlotTop(index) + SLOT_SIZE_Y / 2;
		}
	}
}
