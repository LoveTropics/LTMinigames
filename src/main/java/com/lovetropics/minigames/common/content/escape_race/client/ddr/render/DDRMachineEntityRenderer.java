package com.lovetropics.minigames.common.content.escape_race.client.ddr.render;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.client.ddr.DdrScreen;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DDRMachineEntity;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DdrInput;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DdrLevel;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.TimedDdrInput;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

import java.util.ArrayList;
import java.util.List;

public class DDRMachineEntityRenderer extends EntityRenderer<DDRMachineEntity, DDRMachineRenderState> {
	private static final Identifier TEXTURE = LoveTropics.id("textures/entity/ddr_machine.png");
	private static final float SELECTED_ITEM_SCALE = 1.25f;

	private static final int INPUT_SIDE_WIDTH = 7 * 2;
	private static final int INPUT_SIDE_HEIGHT = 9 * 2;
	private static final int INPUT_UP_WIDTH = 9 * 2;
	private static final int INPUT_UP_HEIGHT = 7 * 2;
	private static final int INPUT_HEIGHT = INPUT_SIDE_HEIGHT;

	private static final int INPUT_SPACING = 24;
	private static final int INPUT_BOTTOM_MARGIN = 3;

	private final DDRMachineEntityModel model;
	private final ItemModelResolver itemModelResolver;
	private final Font font;
	private final DdrScreen screen;

	public DDRMachineEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		model = new DDRMachineEntityModel(context.bakeLayer(DDRMachineEntityModel.LAYER_LOCATION));
		itemModelResolver = context.getItemModelResolver();
		font = context.getFont();
		screen = new DdrScreen(model);
	}

	public DdrScreen getScreen() {
		return screen;
	}

	@Override
	public DDRMachineRenderState createRenderState() {
		return new DDRMachineRenderState();
	}

	@Override
	public void extractRenderState(DDRMachineEntity entity, DDRMachineRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.yRot = entity.getYRot();
		state.toBedState.copyFrom(entity.toBedState);
		state.toDDRState.copyFrom(entity.toDDRMachineState);
		state.ddrMachineState = entity.getState();
		state.input = entity.getClientCurrentInput();
		state.upcomingMoves.clear();
		long currentTick = entity.getClientCurrentTick();
		for (TimedDdrInput input : entity.clientPendingInputs()) {
			long tick = input.tick();
			if ((tick + 10) >= currentTick && tick - currentTick <= 20 * 4) {
				state.upcomingMoves.add(input);
			}
		}
		state.currentTick = currentTick;
		state.isRiding = entity.getControllingPassenger() instanceof LocalPlayer;

		Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
		List<Holder<DdrLevel>> levels = entity.getOrderedLevels();
		if (state.levels.size() != levels.size()) {
			// This practically won't ever happen, as the list of levels is constant for a session
			state.levels = new ArrayList<>(levels.size());
			for (int i = 0; i < levels.size(); i++) {
				state.levels.add(new DDRMachineLevelClientRenderState());
			}
		}

		int pickedLevel = state.isRiding ? screen.pickLevelIndex(camera, entity.position(), state.yRot, levels.size()) : DdrScreen.NO_LEVEL_PICKED;

		for (int i = 0; i < levels.size(); i++) {
			DdrLevel level = levels.get(i).value();
			DDRMachineLevelClientRenderState levelState = state.levels.get(i);
			itemModelResolver.updateForNonLiving(levelState.iconState, level.icon().create(), ItemDisplayContext.FIXED, entity);
			levelState.displayName = level.displayName();
			levelState.selected = i == pickedLevel;
		}
	}

	@Override
	public void submit(DDRMachineRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
		super.submit(renderState, poseStack, submitNodeCollector, camera);

		poseStack.pushPose();
		applyModelTransform(poseStack, renderState.yRot);
		model.setupAnim(renderState);
		submitNodeCollector.submitModel(model, renderState, poseStack, model.renderType(TEXTURE), renderState.lightCoords, OverlayTexture.NO_OVERLAY, renderState.outlineColor, null);
		poseStack.popPose();

		poseStack.pushPose();
		screen.applyTransform(poseStack, renderState.yRot);
		renderScreenContent(poseStack, submitNodeCollector, renderState.lightCoords, renderState);
		poseStack.popPose();
	}

	public static void applyModelTransform(PoseStack poseStack, float yRot) {
		poseStack.scale(-1.0f, -1.0f, 1.0f);
		poseStack.translate(0.0f, EntityModel.MODEL_Y_OFFSET, 0.0f);
		poseStack.mulPose(Axis.YP.rotationDegrees(180.0f + yRot));
	}

	private void renderScreenContent(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight, DDRMachineRenderState state) {
		switch (state.ddrMachineState) {
			case DDRMachineEntity.DDRMachineState.MENU -> renderMenuScreen(poseStack, submitNodeCollector, packedLight, state);
			case DDRMachineEntity.DDRMachineState.PLAYING, DDRMachineEntity.DDRMachineState.RECORDING -> renderPlayingScreen(poseStack, submitNodeCollector, packedLight, state);
		}
	}

	private void renderMenuScreen(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight, DDRMachineRenderState state) {
		DdrScreen.LevelArrangement arrangement = DdrScreen.LevelArrangement.forCount(state.levels.size());
		for (int i = 0; i < state.levels.size(); i++) {
			renderLevelIcon(poseStack, submitNodeCollector, packedLight, state.levels.get(i), arrangement.getCenterX(i), arrangement.getCenterY(i), state);
		}
	}

	private void renderLevelIcon(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight, DDRMachineLevelClientRenderState level, int x, int y, DDRMachineRenderState state) {
		poseStack.pushPose();
		poseStack.translate(x, y, 0.0f);

		poseStack.pushPose();
		poseStack.scale(16.0f, -16.0f, -16.0f);
		if (level.selected) {
			poseStack.scale(SELECTED_ITEM_SCALE, SELECTED_ITEM_SCALE, SELECTED_ITEM_SCALE);
			level.iconState.submit(poseStack, submitNodeCollector, packedLight, OverlayTexture.NO_OVERLAY, ARGB.color(255, 255, 255, 255));
		} else {
			level.iconState.submit(poseStack, submitNodeCollector, packedLight, OverlayTexture.NO_OVERLAY, state.outlineColor);
		}
		poseStack.popPose();

		float textScale = 0.5f;
		poseStack.scale(textScale, textScale, -textScale);

		int backgroundColor = ARGB.color(Minecraft.getInstance().options.getBackgroundOpacity(0.25f), CommonColors.BLACK);
		submitNodeCollector.submitText(
				poseStack,
				-font.width(level.displayName) / 2.0f,
				16.0f,
				level.displayName.getVisualOrderText(),
				false,
				Font.DisplayMode.NORMAL,
				state.lightCoords,
				CommonColors.WHITE,
				backgroundColor,
				state.outlineColor
		);

		poseStack.popPose();
	}

	private void renderPlayingScreen(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight, DDRMachineRenderState state) {
		DDRMachineSprites sprites = DDRMachineSprites.get();

		int leftX = (DdrScreen.WIDTH - INPUT_SPACING * 4 + INPUT_SPACING) / 2;
		int forwardX = leftX + INPUT_SPACING;
		int backX = forwardX + INPUT_SPACING;
		int rightX = backX + INPUT_SPACING;

		int bottomRowCenterY = DdrScreen.HEIGHT - INPUT_BOTTOM_MARGIN - INPUT_HEIGHT / 2;

		for (TimedDdrInput move : state.upcomingMoves) {
			long tick = move.tick();
			int delay = SharedConstants.TICKS_PER_SECOND * 4;
			if (tick - state.currentTick > delay) {
				continue;
			}
			poseStack.translate(0.0f, 0.0f, -0.001f);
			DdrInput levelTick = move.input();
			float yPos = Mth.lerp((float) (tick - state.currentTick) / delay, bottomRowCenterY, INPUT_HEIGHT / 2.0f);
			if (levelTick.left()) {
				drawCenteredSprite(sprites.leftFilled(), leftX, yPos, INPUT_SIDE_WIDTH, INPUT_SIDE_HEIGHT, poseStack, submitNodeCollector, packedLight);
			}
			if (levelTick.right()) {
				drawCenteredSprite(sprites.rightFilled(), rightX, yPos, INPUT_SIDE_WIDTH, INPUT_SIDE_HEIGHT, poseStack, submitNodeCollector, packedLight);
			}
			if (levelTick.forward()) {
				drawCenteredSprite(sprites.upFilled(), forwardX, yPos, INPUT_UP_WIDTH, INPUT_UP_HEIGHT, poseStack, submitNodeCollector, packedLight);
			}
			if (levelTick.back()) {
				drawCenteredSprite(sprites.downFilled(), backX, yPos, INPUT_UP_WIDTH, INPUT_UP_HEIGHT, poseStack, submitNodeCollector, packedLight);
			}
		}

		drawCenteredSprite(state.input.left() ? sprites.leftFilled() : sprites.left(), leftX, bottomRowCenterY, INPUT_SIDE_WIDTH, INPUT_SIDE_HEIGHT, poseStack, submitNodeCollector, packedLight);
		drawCenteredSprite(state.input.forward() ? sprites.upFilled() : sprites.up(), forwardX, bottomRowCenterY, INPUT_UP_WIDTH, INPUT_UP_HEIGHT, poseStack, submitNodeCollector, packedLight);
		drawCenteredSprite(state.input.back() ? sprites.downFilled() : sprites.down(), backX, bottomRowCenterY, INPUT_UP_WIDTH, INPUT_UP_HEIGHT, poseStack, submitNodeCollector, packedLight);
		drawCenteredSprite(state.input.right() ? sprites.rightFilled() : sprites.right(), rightX, bottomRowCenterY, INPUT_SIDE_WIDTH, INPUT_SIDE_HEIGHT, poseStack, submitNodeCollector, packedLight);
	}

	private void drawCenteredSprite(TextureAtlasSprite sprite, float x, float y, int width, int height, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight) {
		drawSprite(sprite, x - width / 2.0f, y - height / 2.0f, width, height, poseStack, submitNodeCollector, packedLight);
	}

	private void drawSprite(TextureAtlasSprite sprite, float x, float y, int width, int height, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight) {
		submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.text(sprite.atlasLocation()), (pose, consumer) -> {
			consumer.addVertex(pose, x, y + height, 0.0f)
					.setColor(CommonColors.WHITE)
					.setUv(sprite.getU0(), sprite.getV1())
					.setLight(packedLight);
			consumer.addVertex(pose, x + width, y + height, 0.0f)
					.setColor(CommonColors.WHITE)
					.setUv(sprite.getU1(), sprite.getV1())
					.setLight(packedLight);
			consumer.addVertex(pose, x + width, y, 0.0f)
					.setColor(CommonColors.WHITE)
					.setUv(sprite.getU1(), sprite.getV0())
					.setLight(packedLight);
			consumer.addVertex(pose, x, y, 0.0f)
					.setColor(CommonColors.WHITE)
					.setUv(sprite.getU0(), sprite.getV0())
					.setLight(packedLight);
		});
	}
}
