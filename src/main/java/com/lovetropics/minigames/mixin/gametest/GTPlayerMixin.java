package com.lovetropics.minigames.mixin.gametest;

import com.lovetropics.minigames.common.util.LTGameTestFakePlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayer.class)
public class GTPlayerMixin {
//	@Redirect(method = "tickRegeneration", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"))
//	private <T> T shouldRegenerate(GameRules instance, GameRule<T> gameRule) {
//		if (this instanceof LTGameTestFakePlayer player && gameRule == GameRules.NATURAL_HEALTH_REGENERATION) {
//			return player.shouldRegenerateNaturally();
//		}
//		return instance.getBoolean(pKey);
//	}
}
