package com.lovetropics.minigames.common.content.escape_race.vending_machine;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.content.escape_race.EscapeRaceTexts;
import com.lovetropics.minigames.common.content.escape_race.client.EscapeRaceClientBucksState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.ArrayList;
import java.util.List;

public class VendingMachineEntityRenderer extends EntityRenderer<VendingMachineEntity, VendingMachineRenderState> {
	private static final ResourceLocation TEXTURE = LoveTropics.location("textures/entity/vending_machine.png");

	private final VendingMachineModel model;
	private final ItemModelResolver itemModelResolver;
	private final Font font;

	public VendingMachineEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		model = new VendingMachineModel(context.bakeLayer(VendingMachineModel.LAYER_LOCATION));
		itemModelResolver = context.getItemModelResolver();
		font = context.getFont();
	}

	public VendingMachineModel getModel() {
		return model;
	}

	@Override
	public VendingMachineRenderState createRenderState() {
		return new VendingMachineRenderState();
	}

	@Override
	public void extractRenderState(VendingMachineEntity entity, VendingMachineRenderState reusedState, float partialTick) {
		super.extractRenderState(entity, reusedState, partialTick);
		Minecraft minecraft = Minecraft.getInstance();
		Camera camera = minecraft.gameRenderer.getMainCamera();
		VendingMachineSlots.Picker picker = VendingMachineSlots.picker(camera, entity);
		int pickedSlot = picker.pickSlot();
		int selectedSlot = minecraft.crosshairPickEntity == entity ? entity.getSelected() : VendingMachineEntity.NO_SLOT;
		reusedState.yRot = entity.getYRot();
		List<ItemStack> visualItems = entity.getVisualItems();
		for (int i = 0; i < reusedState.slots.size(); i++) {
			VendingMachineRenderState.SlotState slotState = reusedState.slots.get(i);
			ItemStack itemStack = i < visualItems.size() ? visualItems.get(i) : ItemStack.EMPTY;
			slotState.update(itemModelResolver, itemStack, entity, selectedSlot == i, pickedSlot == i);
		}
		ItemStack droppingItem = entity.getDroppingItem();
		if (droppingItem != null) {
			itemModelResolver.updateForNonLiving(reusedState.droppingItem, entity.getDroppingItem(), ItemDisplayContext.FIXED, entity);
			reusedState.droppingFromSlot = entity.getDroppingFromSlot();
			reusedState.droppingItemProgress = entity.getDroppingItemProgress(partialTick);
		} else {
			reusedState.droppingItem.clear();
		}
		reusedState.hasSelection = selectedSlot != VendingMachineEntity.NO_SLOT;
		reusedState.anyHighlighted = pickedSlot != VendingMachineEntity.NO_SLOT && !visualItems.get(pickedSlot).isEmpty();
		reusedState.buyButtonPicked = picker.isPicked(model.buyButtonBounds());
	}

	@Override
	public void render(VendingMachineRenderState renderState, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		super.render(renderState, poseStack, bufferSource, packedLight);
		poseStack.pushPose();
		VendingMachineModel.applyModelTransform(poseStack, renderState.yRot);

		model.setupAnim(renderState);
		VertexConsumer builder = bufferSource.getBuffer(model.renderType(TEXTURE));
		model.renderToBuffer(poseStack, builder, packedLight, OverlayTexture.NO_OVERLAY);

		if (renderState.hasSelection) {
			OutlineBufferSource outlineBufferSource = Minecraft.getInstance().renderBuffers().outlineBufferSource();
			outlineBufferSource.setColor(0, 255, 0, 255);
			model.renderBuyButtonOnly(poseStack, outlineBufferSource.getBuffer(model.renderType(TEXTURE)));
		}

		for (int slotIndex = 0; slotIndex < renderState.slots.size(); slotIndex++) {
			VendingMachineRenderState.SlotState slotState = renderState.slots.get(slotIndex);
			if (slotState.item.isEmpty()) {
				continue;
			}
			poseStack.pushPose();
			poseStack.translate(slotState.pos);
			poseStack.scale(-1.0f, -1.0f, 1.0f);
			renderSlot(poseStack, bufferSource, packedLight, slotState.selected, slotState.picked, renderState.anyHighlighted, slotState);
			poseStack.popPose();
		}

		if (!renderState.droppingItem.isEmpty()) {
			VendingMachineRenderState.SlotState fromSlot = renderState.slots.get(renderState.droppingFromSlot);
			renderDroppingItem(renderState, poseStack, bufferSource, packedLight, renderState.droppingItem, fromSlot);
		}

		poseStack.popPose();
	}

	private void renderSlot(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, boolean selected, boolean highlighted, boolean anyHighlighted, VendingMachineRenderState.SlotState slot) {
		float itemScale = computeItemScale(slot.item);

		poseStack.pushPose();
		poseStack.scale(itemScale, itemScale, itemScale);

		renderBackgroundSlotItems(poseStack, bufferSource, packedLight, slot);

		if (selected || highlighted) {
			poseStack.pushPose();
			poseStack.scale(1.25f, 1.25f, 1.25f);
			OutlineBufferSource outlineBufferSource = Minecraft.getInstance().renderBuffers().outlineBufferSource();
			if (selected) {
				outlineBufferSource.setColor(0, 255, 0, 255);
			} else {
				outlineBufferSource.setColor(255, 255, 255, 255);
			}
			slot.item.render(poseStack, outlineBufferSource, packedLight, OverlayTexture.NO_OVERLAY);
			poseStack.popPose();
		} else {
			slot.item.render(poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
		}

		poseStack.popPose();

		// TODO: Big hack to force the text to render in front!
		if (bufferSource instanceof MultiBufferSource.BufferSource b) {
			b.endBatch();
		}

		if ((selected && !anyHighlighted) || highlighted) {
			int backgroundColor = ARGB.color(Minecraft.getInstance().options.getBackgroundOpacity(0.25f), CommonColors.BLACK);
			poseStack.pushPose();
			float scale = 0.15f / 16.0f;
			poseStack.scale(-scale, -scale, scale);
			font.drawInBatch(
					slot.name,
					-font.width(slot.name) / 2.0f,
					-25.0f,
					CommonColors.WHITE,
					false,
					poseStack.last().pose(),
					bufferSource,
					Font.DisplayMode.SEE_THROUGH,
					backgroundColor,
					packedLight
			);
			poseStack.popPose();
		}
	}

	private void renderBackgroundSlotItems(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, VendingMachineRenderState.SlotState slot) {
		poseStack.pushPose();
		for (int i = 0; i < 3; i++) {
			poseStack.translate(0, 0, 0.5);
			slot.item.render(poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
		}
		poseStack.popPose();
	}

	private void renderDroppingItem(VendingMachineRenderState renderState, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, ItemStackRenderState item, VendingMachineRenderState.SlotState fromSlot) {
		poseStack.pushPose();

		float pushAmount = 0.15f;
		float fallToY = 1.0f;
		float distanceToFall = (float) (fallToY - fromSlot.pos.y);

		float pushEndTime = 0.5f;
		float fallDuration = 0.5f * distanceToFall / 1.6f;

		float pushAlpha = Mth.clampedMap(renderState.droppingItemProgress, 0.0f, pushEndTime, 0.0f, 1.0f);
		float fallAlpha = Mth.square(Mth.clampedMap(renderState.droppingItemProgress, pushEndTime, 1.0f, 0.0f, 1.0f));

		float itemScale = computeItemScale(item) * 1.25f;

		float y = (float) Mth.lerp(Math.min(fallAlpha / fallDuration, 1.0f), fromSlot.pos.y, fallToY);
		float z = (float) fromSlot.pos.z + pushAlpha * -pushAmount;
		float yRot = Mth.lerp(fallAlpha, 0, Mth.TWO_PI);

		poseStack.translate(fromSlot.pos.x, y, z);
		poseStack.mulPose(Axis.XN.rotation(yRot));
		poseStack.scale(-itemScale, -itemScale, itemScale);
		item.render(poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
		poseStack.popPose();
	}

	private float computeItemScale(ItemStackRenderState item) {
		double itemSize = item.getModelBoundingBox().getSize();
		float itemScale = 0.4f;
		if (itemSize > 0.5) {
			float diffInSize = (float) itemSize - 0.5f; // Account for different item model scales (ish)
			itemScale = itemScale - diffInSize;
		}
		return itemScale;
	}

	public static void registerOverlays(RegisterGuiLayersEvent event) {
		event.registerAbove(VanillaGuiLayers.EXPERIENCE_LEVEL, LoveTropics.location("vending_machine_info"), (graphics, deltaTracker) -> {
			if (Minecraft.getInstance().options.hideGui) {
				return;
			}
			renderOverlay(graphics);
		});
	}

	private static void renderOverlay(GuiGraphics graphics) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!(minecraft.hitResult instanceof EntityHitResult entityHitResult)) {
			return;
		}
		if (!(entityHitResult.getEntity() instanceof VendingMachineEntity entity)) {
			return;
		}

		Camera camera = minecraft.gameRenderer.getMainCamera();
		VendingMachineSlots.Picker picker = VendingMachineSlots.picker(camera, entity);

		int pickedSlot = picker.pickSlot();
		ItemStack itemStack = pickedSlot != VendingMachineEntity.NO_SLOT ? entity.getVisualItems().get(pickedSlot) : ItemStack.EMPTY;
		if (itemStack.isEmpty()) {
			return;
		}

		List<Component> lines = new ArrayList<>();
		lines.add(itemStack.getHoverName());

		EscapeRaceClientBucksState breakBucks = ClientGameStateManager.getOrNull(EscapeRace.BREAK_BUCK_STATE);
		if (breakBucks != null) {
			int cost = itemStack.getOrDefault(EscapeRace.VENDING_MACHINE_COST, 0);
			Component styledCost = Component.literal(String.valueOf(cost)).withStyle(breakBucks.amount() >= cost ? ChatFormatting.GREEN : ChatFormatting.RED);
			lines.add(EscapeRaceTexts.BREAK_BUCKS_COST.apply(styledCost).withStyle(ChatFormatting.GRAY));
		}

		Font font = minecraft.font;

		int width = lines.stream().mapToInt(font::width).max().orElse(0);
		int height = (font.lineHeight + 1) * lines.size();

		int centerX = graphics.guiWidth() / 2;
		int centerY = graphics.guiHeight() - 70;

		int left = centerX - width / 2;
		int top = centerY - height / 2;

		int padding = 2;
		TooltipRenderUtil.renderTooltipBackground(graphics, left - padding, top - padding, width + padding * 2, height + padding, null);

		for (Component line : lines) {
			graphics.drawCenteredString(font, line, centerX, top, CommonColors.WHITE);
			top += font.lineHeight + 1;
		}
	}
}
