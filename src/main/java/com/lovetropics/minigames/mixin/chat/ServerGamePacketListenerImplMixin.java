package com.lovetropics.minigames.mixin.chat;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.impl.GamePhaseManager;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
	@Shadow
	public ServerPlayer player;

	@Inject(method = "broadcastChatMessage", at = @At("HEAD"), cancellable = true)
	private void broadcastChatMessage(PlayerChatMessage message, CallbackInfo ci) {
		IGamePhase game = GamePhaseManager.get().getGamePhaseFor(player);
		if (game != null && game.invoker(GamePlayerEvents.CHAT).onChat(player, message)) {
			ci.cancel();
		}
	}
}
