package com.lovetropics.minigames.mixin.gametest;

import com.lovetropics.minigames.common.util.LTGameTestFakePlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPlayer.class)
public class GTPlayerMixin {
    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"), method = "tickRegeneration")
    private boolean shouldRegenerate(GameRules instance, GameRules.Key<GameRules.BooleanValue> pKey) {
        if (this instanceof LTGameTestFakePlayer player && pKey == GameRules.RULE_NATURAL_REGENERATION) {
            return player.shouldRegenerateNaturally();
        }
        return instance.getBoolean(pKey);
    }
}
