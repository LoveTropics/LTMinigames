package com.lovetropics.minigames.mixin;

import com.lovetropics.minigames.common.util.duck.ClearableFluidInteraction;
import net.minecraft.world.entity.EntityFluidInteraction;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityFluidInteraction.class)
public abstract class EntityFluidInteractionMixin implements ClearableFluidInteraction {
	@Shadow
	protected abstract EntityFluidInteraction.Tracker getTrackerFor(FluidType fluid);

	@Override
	public void ltminigames$removeFluid(FluidType type) {
		getTrackerFor(type).reset();
	}
}
