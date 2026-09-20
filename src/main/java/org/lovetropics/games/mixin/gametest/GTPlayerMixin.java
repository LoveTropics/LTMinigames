package org.lovetropics.games.mixin.gametest;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;

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
