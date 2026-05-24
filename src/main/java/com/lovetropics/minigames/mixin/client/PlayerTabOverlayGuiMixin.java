package com.lovetropics.minigames.mixin.client;

import com.lovetropics.minigames.client.lobby.state.ClientLobbyManager;
import com.lovetropics.minigames.client.lobby.state.ClientLobbyState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(PlayerTabOverlay.class)
public class PlayerTabOverlayGuiMixin {
	@Inject(method = "decorateName", at = @At("HEAD"), cancellable = true)
	private void getDisplayName(PlayerInfo info, MutableComponent displayName, CallbackInfoReturnable<Component> ci) {
		ClientLobbyState lobby = ClientLobbyManager.getJoined();
		if (lobby != null && lobby.getCurrentGame() != null) {
			UUID id = info.getProfile().id();
			if (lobby.getPlayers().contains(id)) {
				if (info.getGameMode() != GameType.SPECTATOR) {
					ci.setReturnValue(Component.literal("\uD83D\uDDE1 ").append(displayName));
				} else {
					ci.setReturnValue(displayName);
				}
			} else {
				ci.setReturnValue(displayName.withStyle(ChatFormatting.DARK_GRAY));
			}
		}
	}
}
