package com.lovetropics.minigames.client.map;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.Constants;
import com.lovetropics.minigames.common.core.map.workspace.ClientWorkspaceRegions;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

@Mod.EventBusSubscriber(modid = Constants.MODID, value = Dist.CLIENT)
public final class MapWorkspaceRenderer {
	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
			return;
		}

		ClientWorkspaceRegions regions = ClientMapWorkspace.INSTANCE.getRegions();
		if (regions.isEmpty()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		Camera camera = client.gameRenderer.getMainCamera();
		if (!camera.isInitialized()) {
			return;
		}

		Vec3 view = camera.getPosition();

		PoseStack poseStack = event.getPoseStack();
		MultiBufferSource.BufferSource bufferSource = client.renderBuffers().bufferSource();

		Set<ClientWorkspaceRegions.Entry> selectedRegions = MapWorkspaceTracer.getSelectedRegions();

		for (ClientWorkspaceRegions.Entry entry : regions) {
			int color = colorForKey(entry.key);
			float red = (color >> 16 & 0xFF) / 255.0F;
			float green = (color >> 8 & 0xFF) / 255.0F;
			float blue = (color & 0xFF) / 255.0F;
			float outlineRed = red;
			float outlineGreen = green;
			float outlineBlue = blue;

			float alpha = 0.3F;

			if (selectedRegions.contains(entry)) {
				double time = client.level.getGameTime() + event.getPartialTick();
				float animation = (float) ((Math.sin(time * 0.1) + 1.0) / 2.0);

				alpha = 0.4F + animation * 0.15F;

				red = Math.min(red * 1.3F, 1.0F);
				green = Math.min(green * 1.3F, 1.0F);
				blue = Math.min(blue * 1.3F, 1.0F);
				outlineRed = outlineGreen = outlineBlue = 1.0F;
			}

			BlockBox region = entry.region;
			double minX = region.min().getX() - view.x;
			double minY = region.min().getY() - view.y;
			double minZ = region.min().getZ() - view.z;
			double maxX = region.max().getX() + 1.0 - view.x;
			double maxY = region.max().getY() + 1.0 - view.y;
			double maxZ = region.max().getZ() + 1.0 - view.z;

			DebugRenderer.renderFilledBox(poseStack, bufferSource, minX, minY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
			LevelRenderer.renderLineBox(poseStack, bufferSource.getBuffer(RenderType.lines()), minX, minY, minZ, maxX, maxY, maxZ, outlineRed, outlineGreen, outlineBlue, 1.0F);
		}

		for (ClientWorkspaceRegions.Entry entry : regions) {
			Vec3 center = entry.region.center();
			BlockPos size = entry.region.size();

			int minSize = Math.min(size.getX(), Math.min(size.getY(), size.getZ())) - 1;
			float scale = Mth.clamp(minSize * 0.03125F, 0.03125F, 0.125F);

			DebugRenderer.renderFloatingText(poseStack, bufferSource, entry.key, center.x, center.y, center.z, 0xFFFFFFFF, scale);
		}
	}

	private static int colorForKey(String key) {
		return HashCommon.mix(key.hashCode()) & 0xFFFFFF;
	}
}
