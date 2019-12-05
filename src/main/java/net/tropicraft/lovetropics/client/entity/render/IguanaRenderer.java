package net.tropicraft.lovetropics.client.entity.render;

import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.util.ResourceLocation;
import net.tropicraft.lovetropics.Constants;
import net.tropicraft.lovetropics.client.entity.model.IguanaModel;
import net.tropicraft.lovetropics.common.entity.neutral.IguanaEntity;

public class IguanaRenderer extends MobRenderer<IguanaEntity, IguanaModel> {
    private static final String IGOR = "igor";

    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation(Constants.MODID, "textures/entity/iggy.png");
    private static final ResourceLocation IGOR_TEXTURE = new ResourceLocation(Constants.MODID, "textures/entity/iggy_igor.png");

    public IguanaRenderer(final EntityRendererManager rendererManager) {
        super(rendererManager, new IguanaModel(), 0.5F);
        this.shadowOpaque = 0.5f;
    }

    @Override
    protected ResourceLocation getEntityTexture(final IguanaEntity entity) {
        if (entity.getName().getString().equalsIgnoreCase(IGOR)) {
            return IGOR_TEXTURE;
        }

        return DEFAULT_TEXTURE;
    }
}
