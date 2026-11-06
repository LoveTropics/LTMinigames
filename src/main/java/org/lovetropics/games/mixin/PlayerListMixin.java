package org.lovetropics.games.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.lovetropics.games.common.core.game.IGameLookup;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @WrapOperation(method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;sendChatMessage(Lnet/minecraft/network/chat/OutgoingChatMessage;ZLnet/minecraft/network/chat/ChatType$Bound;)V"))
	private void onBroadcastChatMessage(ServerPlayer receiver, OutgoingChatMessage message, boolean filtered, ChatType.Bound chatType, Operation<Void> original, @Local(name = "senderPlayer", argsOnly = true) ServerPlayer senderPlayer) {
		IGamePhase game = IGameLookup.get().getGamePhaseFor(receiver);
		if (game == null) {
			original.call(receiver, message, filtered, chatType);
			return;
		}
		if ( game.invoker(GamePlayerEvents.CHAT_SEND).onChatSend(senderPlayer, receiver)) {
			original.call(receiver, message, filtered, chatType);
		}
	}
}
