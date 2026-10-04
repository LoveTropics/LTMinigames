package org.lovetropics.dimensions.mixin.clock;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;
import org.lovetropics.dimensions.SharedDimensionState;
import org.lovetropics.dimensions.duck.SharedDimensionStateBindable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(ServerClockManager.class)
public class ServerClockManagerMixin implements SharedDimensionStateBindable {
	@Shadow
	@Final
	private Map<Holder<WorldClock>, ?> clocks;

	@Unique
	private @Nullable SharedDimensionState ltdimensions$sharedState;

	@Override
	public void ltdimensions$bindTo(SharedDimensionState sharedState) {
		ltdimensions$sharedState = sharedState;
		for (Object clock : clocks.values()) {
			((SharedDimensionStateBindable) clock).ltdimensions$bindTo(sharedState);
		}
	}

	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getGlobalGameRules()Lnet/minecraft/world/level/gamerules/GameRules;"))
	private GameRules useLevelGameRules(MinecraftServer server, Operation<GameRules> original) {
		return ltdimensions$sharedState != null ? ltdimensions$sharedState.gameRules() : original.call(server);
	}

	@WrapOperation(method = "modifyClock", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V"))
	private void broadcastToPlayersUsingClocks(PlayerList playerList, Packet<?> packet, Operation<Void> original) {
		for (ServerPlayer player : playerList.getPlayers()) {
			if (player.level().clockManager() == (Object) this) {
				player.connection.send(packet);
			}
		}
	}
}
