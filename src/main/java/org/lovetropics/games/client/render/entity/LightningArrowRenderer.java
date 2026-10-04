package org.lovetropics.games.client.render.entity;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TippableArrowRenderer;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.resources.Identifier;
import org.lovetropics.games.common.content.survive_the_tide.entity.LightningArrowEntity;

public class LightningArrowRenderer extends ArrowRenderer<LightningArrowEntity, ArrowRenderState> {
	public LightningArrowRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ArrowRenderState createRenderState() {
		return new ArrowRenderState();
	}

	@Override
	protected Identifier getTextureLocation(ArrowRenderState renderState) {
		return TippableArrowRenderer.NORMAL_ARROW_LOCATION;
	}
}
