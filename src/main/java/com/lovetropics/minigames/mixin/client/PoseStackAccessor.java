package com.lovetropics.minigames.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PoseStack.class)
public interface PoseStackAccessor {
	@Accessor
	void setLastIndex(int index);

	@Accessor
	int getLastIndex();
}
