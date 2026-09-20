package org.lovetropics.games.lobbies.mixin;

import org.lovetropics.games.common.util.duck.ServerPlayerExtension;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import org.lovetropics.games.lobbies.GameLobbyManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player implements ServerPlayerExtension {
	private ServerPlayerMixin(Level level, GameProfile gameProfile) {
		super(level, gameProfile);
	}

	@Inject(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;", at = @At("HEAD"), cancellable = true)
	private void teleportTo(TeleportTransition transition, CallbackInfoReturnable<ServerPlayer> ci) {
		ServerPlayer newPlayer = GameLobbyManager.onPlayerTeleport((ServerPlayer) (Object) this, transition);
		if (newPlayer != null) {
			ci.setReturnValue(newPlayer);
		}
	}
}
