package com.lovetropics.minigames.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface EntityAccessor {
	// Todo 26.1 Port
//	@Invoker("updateInWaterStateAndDoFluidPushing")
//	boolean invokeUpdateInWaterStateAndDoFluidPushing();
}
