package com.lovetropics.minigames.common.content.river_race.render;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.content.river_race.RiverRace;
import com.lovetropics.minigames.common.content.river_race.client_state.RiverRaceClientBarState;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.joml.Matrix3x2fStack;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public final class RiverRaceBarRenderer {
	private static final int MAP_TOP = 3;

	private static final int MAP_WIDTH = 373;
	private static final int MAP_HEIGHT = 24;
	private static final int MAP_MARGIN_X = 4;
	private static final int MAP_MARGIN_Y = 4;

	private static final int BAR_HEIGHT = 2;

	private static final int POINTER_SIZE = 5;

	private static final Identifier MAP_SPRITE = LoveTropics.id("minigames/river_race/map");
	private static final Identifier BAR_SPRITE = LoveTropics.id("minigames/river_race/bar");
	private static final Identifier POINTER_TOP_SPRITE = LoveTropics.id("minigames/river_race/pointer_top");
	private static final Identifier POINTER_BOTTOM_SPRITE = LoveTropics.id("minigames/river_race/pointer_bottom");
	private static final Identifier LOCKED_SPRITE = LoveTropics.id("minigames/river_race/locked");

	public static void registerOverlays(RegisterGuiLayersEvent event) {
		event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, LoveTropics.id("river_race_bar"), (graphics, deltaTracker) -> {
			if (Minecraft.getInstance().options.hideGui) {
				return;
			}
			RiverRaceClientBarState barState = ClientGameStateManager.getOrNull(RiverRace.BAR_STATE);
			if (barState != null) {
				render(graphics, barState);
			}
		});
	}

	@SubscribeEvent
	public static void onRenderLayerPre(RenderGuiLayerEvent.Pre event) {
		if (event.getName().equals(VanillaGuiLayers.BOSS_OVERLAY) && ClientGameStateManager.getOrNull(RiverRace.BAR_STATE) != null) {
			Matrix3x2fStack pose = event.getGuiGraphics().pose();
			pose.pushMatrix();
			pose.translate(0.0f, MAP_HEIGHT + POINTER_SIZE);
		}
	}

	@SubscribeEvent
	public static void onRenderLayerPost(RenderGuiLayerEvent.Post event) {
		if (event.getName().equals(VanillaGuiLayers.BOSS_OVERLAY) && ClientGameStateManager.getOrNull(RiverRace.BAR_STATE) != null) {
			event.getGuiGraphics().pose().popMatrix();
		}
	}

	private static void render(GuiGraphicsExtractor graphics, RiverRaceClientBarState barState) {
		int mapLeft = (graphics.guiWidth() - MAP_WIDTH) / 2;
		int left = mapLeft + MAP_MARGIN_X;
		renderTeamMarkers(graphics, barState.topTeam(), left, true);
		renderTeamMarkers(graphics, barState.bottomTeam(), left, false);

		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, MAP_SPRITE, mapLeft, MAP_TOP, MAP_WIDTH, MAP_HEIGHT);

		for (RiverRaceClientBarState.Zone zone : barState.lockedZones()) {
			int color = zone.color().getTextureDiffuseColor();
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LOCKED_SPRITE, left + zone.start(), MAP_TOP + MAP_MARGIN_Y, zone.length(), MAP_HEIGHT - MAP_MARGIN_Y * 2, color);
		}
	}

	private static void renderTeamMarkers(GuiGraphicsExtractor graphics, RiverRaceClientBarState.Team team, int left, boolean top) {
		int barY = top ? MAP_TOP + MAP_MARGIN_Y - BAR_HEIGHT - 1 : MAP_TOP + MAP_HEIGHT - MAP_MARGIN_Y + 1;
		int pointerY = top ? 0 : MAP_TOP + MAP_HEIGHT - MAP_MARGIN_Y + BAR_HEIGHT;

		int teamColor = team.color().getTextureDiffuseColor();
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BAR_SPRITE, left, barY, team.progress(), BAR_HEIGHT, teamColor);

		IntList playerPositions = team.players();
		for (int i = 0; i < playerPositions.size(); i++) {
			int x = playerPositions.getInt(i);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, top ? POINTER_TOP_SPRITE : POINTER_BOTTOM_SPRITE, left + x - POINTER_SIZE / 2 - 1, pointerY, POINTER_SIZE, POINTER_SIZE, teamColor);
		}
	}
}
