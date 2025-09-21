package com.lovetropics.minigames.mixin.gametest;

import com.lovetropics.minigames.common.util.LTGameTestFakePlayer;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerList.class)
public class GTPlayerListMixin {
	@Redirect(at = @At(value = "NEW", target = "(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)Lnet/minecraft/server/network/ServerGamePacketListenerImpl;"), method = "placeNewPlayer")
	private ServerGamePacketListenerImpl listener(MinecraftServer pServer, Connection pConnection, ServerPlayer pPlayer, CommonListenerCookie cookie) {
		if (pPlayer instanceof LTGameTestFakePlayer fp) {
			return new ServerGamePacketListenerImpl(pServer, pConnection, pPlayer, cookie) {
				{
					ObfuscationReflectionHelper.setPrivateValue(Connection.class, connection, new EmbeddedChannel(), "channel");
				}

				@Override
				public void send(Packet<?> packet, @Nullable ChannelFutureListener sendListener) {
					fp.capturePacket(packet);
				}
			};
		}
		return new ServerGamePacketListenerImpl(pServer, pConnection, pPlayer, cookie);
	}
}
