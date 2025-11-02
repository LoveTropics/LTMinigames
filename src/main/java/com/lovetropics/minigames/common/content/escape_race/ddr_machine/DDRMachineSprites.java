package com.lovetropics.minigames.common.content.escape_race.ddr_machine;

import com.lovetropics.minigames.LoveTropics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public class DDRMachineSprites {

	private static final ResourceLocation SCREEN_BG = LoveTropics.location("ddr/bg");
	private static final ResourceLocation SCREEN_DOWN = LoveTropics.location("ddr/down_normal");
	private static final ResourceLocation SCREEN_DOWN_FILLED = LoveTropics.location("ddr/down_filled");
	private static final ResourceLocation SCREEN_UP = LoveTropics.location("ddr/up_normal");
	private static final ResourceLocation SCREEN_UP_FILLED = LoveTropics.location("ddr/up_filled");
	private static final ResourceLocation SCREEN_LEFT = LoveTropics.location("ddr/left_normal");
	private static final ResourceLocation SCREEN_LEFT_FILLED = LoveTropics.location("ddr/left_filled");
	private static final ResourceLocation SCREEN_RIGHT = LoveTropics.location("ddr/right_normal");
	private static final ResourceLocation SCREEN_RIGHT_FILLED = LoveTropics.location("ddr/right_filled");
	private static final ResourceLocation SCREEN_LOGO = LoveTropics.location("ddr/logo");
	public static TextureAtlasSprite bgSprite, downSprite, downFilledSprite, upSprite, upFilledSprite, leftSprite, leftFilledSprite, rightSprite, rightFilledSprite, logoSprite;


	@SubscribeEvent
	public static void onTextureStitched(TextureAtlasStitchedEvent event) {
		if(event.getAtlas().location().equals(ResourceLocation.withDefaultNamespace("textures/atlas/gui.png"))) {
			bgSprite = event.getAtlas().getSprite(SCREEN_BG);
			downSprite = event.getAtlas().getSprite(SCREEN_DOWN);
			downFilledSprite = event.getAtlas().getSprite(SCREEN_DOWN_FILLED);
			upSprite = event.getAtlas().getSprite(SCREEN_UP);
			upFilledSprite = event.getAtlas().getSprite(SCREEN_UP_FILLED);
			leftSprite = event.getAtlas().getSprite(SCREEN_LEFT);
			leftFilledSprite = event.getAtlas().getSprite(SCREEN_LEFT_FILLED);
			rightSprite = event.getAtlas().getSprite(SCREEN_RIGHT);
			rightFilledSprite = event.getAtlas().getSprite(SCREEN_RIGHT_FILLED);
			logoSprite = event.getAtlas().getSprite(SCREEN_LOGO);
		}
	}
}
