package com.lovetropics.minigames.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.lovetropics.minigames.common.content.turtle_race.RiderBehavior;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.commands.RideCommand;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static com.mojang.brigadier.arguments.BoolArgumentType.getBool;

@Mixin(RideCommand.class)
public class RideCommandMixin {
	@Unique
	private static boolean force = false;

	@WrapOperation(method = "register", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/Commands;argument(Ljava/lang/String;Lcom/mojang/brigadier/arguments/ArgumentType;)Lcom/mojang/brigadier/builder/RequiredArgumentBuilder;", ordinal = 1))
	private static RequiredArgumentBuilder<CommandSourceStack, EntityArgument> addForceArgument(String name, ArgumentType<EntityArgument> type, Operation<RequiredArgumentBuilder<CommandSourceStack, EntityArgument>> original) {
		return original.call(name, type)
				.then(
						Commands.argument("force", bool())
								.executes(
										ctx -> mount(
												ctx.getSource(),
												EntityArgument.getEntity(ctx, "target"),
												EntityArgument.getEntity(ctx, "vehicle"),
												getBool(ctx, "force")
										)
								)
				);
	}

	@Unique
	private static int mount(CommandSourceStack source, Entity target, Entity vehicle, boolean force) {
		RideCommandMixin.force = force;
		return mount(source, target, vehicle);
	}

	@Inject(method = "mount", at = @At("TAIL"))
	private static void attachForce(CommandSourceStack source, Entity target, Entity vehicle, CallbackInfoReturnable<Integer> cir) {
		target.setData(RiderBehavior.FORCE_RIDER, force);
		force = false;
	}

	@Inject(method = "dismount", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;stopRiding()V"))
	private static void removeForce(CommandSourceStack source, Entity target, CallbackInfoReturnable<Integer> cir) {
		target.removeData(RiderBehavior.FORCE_RIDER);
	}

	@Shadow
	private static int mount(CommandSourceStack source, Entity target, Entity vehicle) {
		return 0;
	}
}
