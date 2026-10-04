package org.lovetropics.dimensions.mixin.clock;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.clock.ServerClockManager;
import org.lovetropics.dimensions.RuntimeServerLevel;
import org.lovetropics.dimensions.SharedDimensionState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerClockMixin {
	@Shadow
	public abstract Iterable<ServerLevel> getAllLevels();

	@Inject(method = "tickChildren", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/clock/ServerClockManager;tick()V", shift = At.Shift.AFTER))
	private void tickAllClocks(BooleanSupplier haveTime, CallbackInfo ci) {
		// Collect all unique shared states so that we don't double-tick
		Set<SharedDimensionState> sharedStates = new ReferenceArraySet<>();
		for (ServerLevel level : getAllLevels()) {
			if (level instanceof RuntimeServerLevel runtimeLevel) {
				SharedDimensionState sharedState = runtimeLevel.sharedState();
				if (sharedState != null) {
					sharedStates.add(sharedState);
				}
			}
		}
		for (SharedDimensionState sharedState : sharedStates) {
			sharedState.clockManager().tick();
		}
	}

	// gamerule is serverwide, this patches for server map levels
	@WrapOperation(method = "onGameRuleChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V"))
	private void broadcastToPlayersUsingServerClocks(PlayerList playerList, Packet<?> packet, Operation<Void> original) {
		for (ServerPlayer player : playerList.getPlayers()) {
			ServerClockManager clockManager = player.level().clockManager();
			player.connection.send(clockManager.createFullSyncPacket());
		}
	}
}
