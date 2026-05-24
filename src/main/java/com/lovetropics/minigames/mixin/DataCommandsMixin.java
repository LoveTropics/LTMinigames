package com.lovetropics.minigames.mixin;

import com.lovetropics.minigames.common.util.world.gamedata.GameDataAccessor;
import net.minecraft.util.Util;
import net.minecraft.server.commands.data.DataCommands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Function;

@Mixin(DataCommands.class)
public class DataCommandsMixin {
	@Shadow
	@Final
	@Mutable
	public static List<Function<String, DataCommands.DataProvider>> ALL_PROVIDERS;

	@Inject(method = "<clinit>", at = @At(value = "FIELD", target = "Lnet/minecraft/server/commands/data/DataCommands;ALL_PROVIDERS:Ljava/util/List;", shift = At.Shift.AFTER))
	private static void initializeProviders(CallbackInfo ci) {
		ALL_PROVIDERS = Util.copyAndAdd(ALL_PROVIDERS, GameDataAccessor.PROVIDER);
	}
}
