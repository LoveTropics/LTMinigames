package com.lovetropics.minigames.client.gui;

import com.mojang.blaze3d.platform.Window;
import com.mojang.logging.LogUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.util.CommonColors;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@EventBusSubscriber
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
			this.upsideDown = false;
			this.addingX = RANDOM.nextBoolean();
			this.addingY = RANDOM.nextBoolean();

			Window screen = Minecraft.getInstance().getWindow();
			int screenWidth = screen.getGuiScaledWidth();
			int screenHeight = screen.getGuiScaledHeight();

			this.x = randomInt(0, screenWidth - FACE_SIZE);
			this.y = randomInt(0, screenHeight - FACE_SIZE);
		}

		public void renderTick(GuiGraphicsExtractor extractor, int screenWidth, int screenHeight) {
			this.x = this.addingX ? this.x + 1 : this.x - 1;
			this.y = this.addingY ? this.y + 1 : this.y - 1;

			int maxX = screenWidth - FACE_SIZE;
			int maxY = screenHeight - FACE_SIZE;

			if (this.x >= maxX) {
				this.addingX = false;
			} else if (this.x <= 0) {
				this.addingX = true;
			}

			if (this.y >= maxY) {
				this.addingY = false;
			} else if (this.y <= 0) {
				this.addingY = true;
			}

			// Flip faces when the face hits a corner of the screen
			if ((this.x >= maxX || this.x <= 0) && (this.y <= 0 || this.y >= maxY)) {
				this.upsideDown = !this.upsideDown;
			}
			
			PlayerFaceExtractor.extractRenderState(extractor, this.renderInfo.playerSkin().body().texturePath(), this.x, this.y, FACE_SIZE, true, upsideDown, CommonColors.WHITE);
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

