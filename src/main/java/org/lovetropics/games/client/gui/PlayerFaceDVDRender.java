package org.lovetropics.games.client.gui;

import org.lovetropics.games.LoveTropics;
import com.mojang.blaze3d.platform.Window;
import com.mojang.logging.LogUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.util.CommonColors;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public class PlayerFaceDVDRender {

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final List<PlayerData> FACES = new ArrayList<>();

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		FACES.forEach(playerData -> playerData.lengthInTicks--);
		FACES.removeIf(playerData -> playerData.lengthInTicks-- <= 0);
	}

	public static void renderPlayerDVDFaces(GuiGraphicsExtractor graphics, DeltaTracker tracker) {
		Window screen = Minecraft.getInstance().getWindow();

		int screenWidth = screen.getGuiScaledWidth();
		int screenHeight = screen.getGuiScaledHeight();

		for (PlayerData playerData : FACES) {
			playerData.renderTick(graphics, screenWidth, screenHeight);
		}

	}

	public static class PlayerData {

		private static final Random RANDOM = new Random();
		private static final int FACE_SIZE = 16;

		private final PlayerSkinRenderCache.RenderInfo renderInfo;

		private int x;
		private int y;
		private boolean addingX;
		private boolean addingY;
		private int lengthInTicks;
		private boolean upsideDown;

		public PlayerData(int lengthInTicks, PlayerSkinRenderCache.RenderInfo renderInfo) {
			this.renderInfo = renderInfo;
			this.lengthInTicks = lengthInTicks;
			upsideDown = false;
			addingX = RANDOM.nextBoolean();
			addingY = RANDOM.nextBoolean();

			Window screen = Minecraft.getInstance().getWindow();
			int screenWidth = screen.getGuiScaledWidth();
			int screenHeight = screen.getGuiScaledHeight();

			x = randomInt(0, screenWidth - FACE_SIZE);
			y = randomInt(0, screenHeight - FACE_SIZE);
		}

		public void renderTick(GuiGraphicsExtractor extractor, int screenWidth, int screenHeight) {
			x = addingX ? x + 1 : x - 1;
			y = addingY ? y + 1 : y - 1;

			int maxX = screenWidth - FACE_SIZE;
			int maxY = screenHeight - FACE_SIZE;

			if (x >= maxX) {
				addingX = false;
			} else if (x <= 0) {
				addingX = true;
			}

			if (y >= maxY) {
				addingY = false;
			} else if (y <= 0) {
				addingY = true;
			}

			// Flip faces when the face hits a corner of the screen
			if ((x >= maxX || x <= 0) && (y <= 0 || y >= maxY)) {
				upsideDown = !upsideDown;
			}
			
			PlayerFaceExtractor.extractRenderState(extractor, renderInfo.playerSkin().body().texturePath(), x, y, FACE_SIZE, true, upsideDown, CommonColors.WHITE);
		}

		private int randomInt(int min, int max) {
			return RANDOM.nextInt((max - min) + 1) + min;
		}

	}

	public static void add(UUID uuid, int lengthInTicks) {
		Minecraft.getInstance().playerSkinRenderCache()
				.lookup(ResolvableProfile.createUnresolved(uuid))
				.whenComplete((renderInfo, exp) ->
						renderInfo.ifPresentOrElse(
								info -> FACES.add(new PlayerData(lengthInTicks, info)),
								() -> LOGGER.warn("Failed to Player Skin Info for UUID: {}", uuid, exp)
						)
				);
	}

	public static void clear() {
		FACES.clear();
	}

}

