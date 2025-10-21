package com.lovetropics.minigames.mixin.client.disguise;

import com.lovetropics.minigames.client.ClientPlayerDisguises;
import com.lovetropics.minigames.common.core.diguise.PlayerDisguise;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin extends Player {
	public AbstractClientPlayerMixin(Level level, GameProfile gameProfile) {
		super(level, gameProfile);
	}

	@Inject(method = "getSkin", at = @At("HEAD"), cancellable = true)
	private void getSkin(CallbackInfoReturnable<PlayerSkin> cir) {
		PlayerDisguise disguise = PlayerDisguise.getOrNull(this);
		if (disguise != null) {
			ResolvableProfile skinProfile = disguise.type().skinProfile();
			if (skinProfile != null) {
				cir.setReturnValue(ClientPlayerDisguises.getSkin(skinProfile));
			}
		}
	}
}
