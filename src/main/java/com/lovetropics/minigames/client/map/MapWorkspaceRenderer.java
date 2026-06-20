package com.lovetropics.minigames.client.map;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.map.workspace.ClientWorkspaceRegions;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.OptionalDouble;
import java.util.Set;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public final class MapWorkspaceRenderer {
	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent.AfterWeather event) {
		ClientWorkspaceRegions regions = ClientMapWorkspace.INSTANCE.getRegions();
		if (regions.isEmpty()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		Camera camera = client.gameRenderer.mainCamera();
		if (!camera.isInitialized()) {
			return;
		}

		if (!client.level.dimension().identifier().getNamespace().equals(LoveTropics.ID)) {
			// Don't render for survival or adventure players in persistent worlds
			if (!client.player.isCreative() && !client.player.isSpectator()) {
				return;
			}
		}

		Set<ClientWorkspaceRegions.Entry> selectedRegions = MapWorkspaceTracer.getSelectedRegions();

		for (ClientWorkspaceRegions.Entry entry : regions) {
			int color = colorForKey(entry.key);
			float red = ARGB.redFloat(color);
			float green = ARGB.greenFloat(color);
			float blue = ARGB.blueFloat(color);
			float outlineRed = red;
			float outlineGreen = green;
			float outlineBlue = blue;

			float alpha = 0.3F;

			if (selectedRegions.contains(entry)) {
				double time = client.level.getGameTime() + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaTicks();
				float animation = (float) ((Math.sin(time * 0.1) + 1.0) / 2.0);

				alpha = 0.4F + animation * 0.15F;

				red = Math.min(red * 1.3F, 1.0F);
				green = Math.min(green * 1.3F, 1.0F);
				blue = Math.min(blue * 1.3F, 1.0F);
				outlineRed = outlineGreen = outlineBlue = 1.0F;
			}

			BlockBox region = entry.region;
			double minX = region.min().getX();
			double minY = region.min().getY();
			double minZ = region.min().getZ();
			double maxX = region.max().getX() + 1.0;
			double maxY = region.max().getY() + 1.0;
			double maxZ = region.max().getZ() + 1.0;

			AABB aabb = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
			Gizmos.cuboid(aabb, GizmoStyle.stroke(ARGB.colorFromFloat(alpha, red, green, blue)));
			Gizmos.cuboid(aabb, GizmoStyle.fill(ARGB.colorFromFloat(0.5f, outlineRed, outlineGreen, outlineBlue)));
		}

		for (ClientWorkspaceRegions.Entry entry : regions) {
			Vec3 center = entry.region.center();
			BlockPos size = entry.region.size();

			int minSize = Math.min(size.getX(), Math.min(size.getY(), size.getZ())) - 1;
			float scale = Mth.clamp(minSize * TextGizmo.Style.DEFAULT_SCALE, TextGizmo.Style.DEFAULT_SCALE, 0.125F);

			Gizmos.billboardText(entry.key, center, new TextGizmo.Style(CommonColors.WHITE, scale, OptionalDouble.empty()));
		}

	}

	private static int colorForKey(String key) {
		return ARGB.opaque(HashCommon.mix(key.hashCode()) & 0xFFFFFF);
	}
}
