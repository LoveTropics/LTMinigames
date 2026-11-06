package org.lovetropics.games.mixin.fix;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerAdvancements.class)
public class PlayerAdvancementsMixin {
	@WrapOperation(method = "lambda$award$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
	private void sendAwardMessage(PlayerList list, Component message, boolean overlay, Operation<Void> original) {
		for (ServerPlayer player : list.getPlayers()) {
			if (player.level().getGameRules().get(GameRules.SHOW_ADVANCEMENT_MESSAGES)) {
				original.call(list, message, overlay);
			}
		}
	}
}
