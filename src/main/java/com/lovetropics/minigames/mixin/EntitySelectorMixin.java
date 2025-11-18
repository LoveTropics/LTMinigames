package com.lovetropics.minigames.mixin;

import com.lovetropics.minigames.common.core.dimension.RuntimeDimensions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(EntitySelector.class)
public class EntitySelectorMixin {
	@Shadow
	@Final
	private boolean includesEntities;
	@Mutable
	@Shadow
	@Final
	private boolean worldLimited;

	// Reduce the severity of `/tp @e` just a little
	@Inject(method = "findEntities", at = @At("HEAD"))
	private void findEntities(CommandSourceStack source, CallbackInfoReturnable<List<? extends Entity>> cir) {
		RuntimeDimensions runtimeDimensions = RuntimeDimensions.get(source.getServer());
		if (includesEntities && runtimeDimensions.isTemporaryDimension(source.getLevel().dimension())) {
			worldLimited = true;
		}
	}
}
