package com.lovetropics.minigames.common.content.escape_race.client.ddr.render;

import com.lovetropics.minigames.LoveTropics;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;

import org.jspecify.annotations.Nullable;
import java.util.Objects;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public record DDRMachineSprites(
		TextureAtlasSprite down,
		TextureAtlasSprite downFilled,
		TextureAtlasSprite up,
		TextureAtlasSprite upFilled,
		TextureAtlasSprite left,
		TextureAtlasSprite leftFilled,
		TextureAtlasSprite right,
		TextureAtlasSprite rightFilled
) {
	private static @Nullable DDRMachineSprites instance;

	private static final Identifier SCREEN_DOWN = LoveTropics.id("ddr/down_normal");
	private static final Identifier SCREEN_DOWN_FILLED = LoveTropics.id("ddr/down_filled");
	private static final Identifier SCREEN_UP = LoveTropics.id("ddr/up_normal");
	private static final Identifier SCREEN_UP_FILLED = LoveTropics.id("ddr/up_filled");
	private static final Identifier SCREEN_LEFT = LoveTropics.id("ddr/left_normal");
	private static final Identifier SCREEN_LEFT_FILLED = LoveTropics.id("ddr/left_filled");
	private static final Identifier SCREEN_RIGHT = LoveTropics.id("ddr/right_normal");
	private static final Identifier SCREEN_RIGHT_FILLED = LoveTropics.id("ddr/right_filled");

	@SubscribeEvent
	public static void onTextureStitched(TextureAtlasStitchedEvent event) {
		TextureAtlas atlas = event.getAtlas();
		if (!atlas.location().equals(Identifier.withDefaultNamespace("textures/atlas/gui.png"))) {
			return;
		}
		instance = new DDRMachineSprites(
				atlas.getSprite(SCREEN_DOWN),
				atlas.getSprite(SCREEN_DOWN_FILLED),
				atlas.getSprite(SCREEN_UP),
				atlas.getSprite(SCREEN_UP_FILLED),
				atlas.getSprite(SCREEN_LEFT),
				atlas.getSprite(SCREEN_LEFT_FILLED),
				atlas.getSprite(SCREEN_RIGHT),
				atlas.getSprite(SCREEN_RIGHT_FILLED)
		);
	}

	public static DDRMachineSprites get() {
		return Objects.requireNonNull(instance);
	}
}
