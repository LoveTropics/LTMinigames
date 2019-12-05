package net.tropicraft.lovetropics.client.entity.model;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.tropicraft.lovetropics.client.entity.TropicraftSpecialRenderHelper;

public class PlayerHeadpieceRenderer extends BipedModel<LivingEntity> {

	private int textureIndex;
	protected TropicraftSpecialRenderHelper renderer;

	public PlayerHeadpieceRenderer(final int textureIndex) {
		super();
		this.textureIndex = textureIndex;
		renderer = new TropicraftSpecialRenderHelper();
	}

	@Override
	public void render(LivingEntity entity, float f0, float f1, float f2, float f3, float f4, float f5) {
		float rotationYaw = f3;

		if (entity instanceof ArmorStandEntity) {
			rotationYaw = entity.rotationYawHead;
		}

		setRotationAngles(entity, f0, f1, f2, rotationYaw, f4, f5);

		GlStateManager.pushMatrix();

		if (entity.isSneaking()) {
			GlStateManager.translatef(0, 0.25f, 0);
		}

		// Set head rotation to mask
		GlStateManager.rotatef(rotationYaw, 0, 1, 0);
		GlStateManager.rotatef(f4, 1, 0, 0);

		// Flip mask to face away from the player
		GlStateManager.rotatef(180, 0, 1, 0);

		// put it in the middle in front of the face, eyeholes at (Steve's) eye height
		GlStateManager.translatef(0.0F, 0.16F, 0.3F);

   		// renderMask handles the rendering of the mask model, but it doesn't set the texture.
		// Setting the texture is handled in the item class.
		renderer.renderMask(this.textureIndex);

		GlStateManager.popMatrix();
	}
}