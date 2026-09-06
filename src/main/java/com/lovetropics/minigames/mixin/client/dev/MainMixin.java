package com.lovetropics.minigames.mixin.client.dev;

import com.llamalad7.mixinextras.sugar.Local;
import com.lovetropics.minigames.common.dev.DevQuickPlay;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.client.main.Main;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public class MainMixin {
	@Unique
	private static @Nullable OptionSpec<String> lt$quickPlayMinigame;

	@Inject(method = "main", at = @At(value = "INVOKE", target = "Ljoptsimple/OptionParser;parse([Ljava/lang/String;)Ljoptsimple/OptionSet;"))
	private static void injectOptions(String[] args, CallbackInfo ci, @Local OptionParser parser) {
		lt$quickPlayMinigame = parser.accepts(DevQuickPlay.OPTION_NAME).withOptionalArg();
	}

	@Inject(method = "main", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/main/Main;parseArgument(Ljoptsimple/OptionSet;Ljoptsimple/OptionSpec;)Ljava/lang/Object;", ordinal = 0))
	private static void captureOptions(String[] args, CallbackInfo ci, @Local OptionSet optionSet) {
		if (optionSet.has(lt$quickPlayMinigame)) {
			Identifier gameId = Identifier.parse(optionSet.valueOf(lt$quickPlayMinigame));
			DevQuickPlay.setQuickPlayGameId(gameId);
		}
	}
}
