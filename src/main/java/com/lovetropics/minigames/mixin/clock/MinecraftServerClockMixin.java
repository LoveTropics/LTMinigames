package com.lovetropics.minigames.mixin.clock;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.lovetropics.minigames.common.core.map.MapClocks;
import com.lovetropics.minigames.common.core.map.MapWorldInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MinecraftServerClockMixin {
	@Inject(method = "tickChildren", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/clock/ServerClockManager;tick()V", shift = At.Shift.AFTER))
	private void tickMapClocks(BooleanSupplier haveTime, CallbackInfo ci) {
		// Linked levels (e.g. a map's own Nether and End) share their world info, which must only tick once
		Set<MapWorldInfo> tickedInfos = Collections.newSetFromMap(new IdentityHashMap<>());
		for (ServerLevel level : ((MinecraftServer) (Object) this).getAllLevels()) {
			if (level.getLevelData() instanceof MapWorldInfo mapInfo && tickedInfos.add(mapInfo)) {
				mapInfo.tickClocks();
			}
		}
	}

	// gamerule is serverwide, this patches for server map levels
	@WrapOperation(method = "onGameRuleChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V"))
	private void broadcastToPlayersUsingServerClocks(PlayerList playerList, Packet<?> packet, Operation<Void> original) {
		MapClocks.broadcast(playerList, ((MinecraftServer) (Object) this).clockManager(), packet);
	}
}
