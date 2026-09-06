package com.lovetropics.minigames.client.render;

import com.lovetropics.minigames.LoveTropics;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

// LTMinigames Render Types
@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public final class GameRenderTypes {
	private static final RenderPipeline TRANSLUCENT_NO_TEX_PIPELINE = RenderPipelines.DEBUG_QUADS.toBuilder()
			.withLocation(LoveTropics.id("pipeline/translucent_broken_depth"))
			.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true, 1.0f, 10.0f))
			.build();

	private static final RenderPipeline TRANSLUCENT_NO_TEX_NO_OFFSET_PIPELINE = RenderPipelines.DEBUG_QUADS.toBuilder()
			.withLocation(LoveTropics.id("pipeline/translucent_broken_depth_no_offset"))
			.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
			.build();

	public static final RenderType TRANSLUCENT_NO_TEX = RenderType.create(
			"translucent_broken_depth",
			RenderSetup.builder(TRANSLUCENT_NO_TEX_PIPELINE).createRenderSetup()
	);

	public static final RenderType TRANSLUCENT_NO_TEX_NO_OFFSET = RenderType.create(
			"translucent_broken_depth_no_offset",
			RenderSetup.builder(TRANSLUCENT_NO_TEX_NO_OFFSET_PIPELINE).createRenderSetup()
	);

	private GameRenderTypes() {
	}

	@SubscribeEvent
	public static void registerPipelines(RegisterRenderPipelinesEvent event) {
		event.registerPipeline(TRANSLUCENT_NO_TEX_PIPELINE);
		event.registerPipeline(TRANSLUCENT_NO_TEX_NO_OFFSET_PIPELINE);
	}
}
