package com.lovetropics.minigames.mixin.gametest;

import com.lovetropics.minigames.common.util.LTGameTestFakePlayer;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerList.class)
public class GTPlayerListMixin {
    @Redirect(at = @At(value = "NEW", target = "(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)Lnet/minecraft/server/network/ServerGamePacketListenerImpl;"), method = "placeNewPlayer")
    private ServerGamePacketListenerImpl listener(MinecraftServer pServer, Connection pConnection, ServerPlayer pPlayer) {
        if (pPlayer instanceof LTGameTestFakePlayer fp) {
            return new ServerGamePacketListenerImpl(pServer, pConnection, pPlayer) {
                {
                    ObfuscationReflectionHelper.setPrivateValue(Connection.class, this.connection, new EmbeddedChannel(), "channel");
                }

                @Override
                public void send(Packet<?> pPacket, @Nullable PacketSendListener pListener) {
                    fp.capturePacket(pPacket);
                }
            };
        }
        return new ServerGamePacketListenerImpl(pServer, pConnection, pPlayer);
    }
}
