// Todo 26.1 Port
//package com.lovetropics.minigames.client.render;
//
//import com.lovetropics.minigames.LoveTropics;
//import com.mojang.blaze3d.pipeline.RenderPipeline;
//import net.minecraft.client.renderer.RenderPipelines;
//import net.neoforged.api.distmarker.Dist;
//import net.neoforged.bus.api.SubscribeEvent;
//import net.neoforged.fml.common.EventBusSubscriber;
//import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
//
//// LTMinigames Render Types
//// Extends RenderStateShard to access protected fields
//@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
//public class GameRenderTypes extends RenderStateShard {
//	private static final RenderPipeline TRANSLUCENT_NO_TEX_PIPELINE = RenderPipelines.DEBUG_QUADS.toBuilder()
//			.withLocation(LoveTropics.location("translucent_broken_depth"))
//			.withColorWrite(true)
//			.withDepthWrite(false)
//			.withDepthBias(-1.0f, -10.0f)
//			.build();
//
//	public static final RenderType TRANSLUCENT_NO_TEX = create(
//			"translucent_broken_depth",
//			2097152,
//			false,
//			true,
//			TRANSLUCENT_NO_TEX_PIPELINE,
//			RenderType.CompositeState.builder()
//					.setOutputState(TRANSLUCENT_TARGET)
//					.createCompositeState(false)
//	);
//
//	public GameRenderTypes(String pName, Runnable pSetupState, Runnable pClearState) {
//		super(pName, pSetupState, pClearState);
//		throw new IllegalStateException("Don't call this");
//	}
//
//	@SubscribeEvent
//	public static void registerPipelines(RegisterRenderPipelinesEvent event) {
//		event.registerPipeline(TRANSLUCENT_NO_TEX_PIPELINE);
//	}
//}
