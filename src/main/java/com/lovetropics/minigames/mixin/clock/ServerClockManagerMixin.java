package com.lovetropics.minigames.mixin.clock;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.lovetropics.minigames.common.core.map.MapClocks;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(ServerClockManager.class)
public class ServerClockManagerMixin implements MapClocks.Access {
	@Shadow
	@Final
	private Map<Holder<WorldClock>, ?> clocks;

	@Unique
	private @Nullable ServerLevel ltminigames$level;

	@Override
	public void ltminigames$setLevel(ServerLevel level) {
		ltminigames$level = level;
		for (Object clock : clocks.values()) {
			((MapClocks.Access) clock).ltminigames$setLevel(level);
		}
	}

	@Override
	public @Nullable ServerLevel ltminigames$getLevel() {
		return ltminigames$level;
	}

	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getGlobalGameRules()Lnet/minecraft/world/level/gamerules/GameRules;"))
	private GameRules useLevelGameRules(MinecraftServer server, Operation<GameRules> original) {
		return ltminigames$level != null ? ltminigames$level.getGameRules() : original.call(server);
	}

	@WrapOperation(method = "modifyClock", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V"))
	private void broadcastToPlayersUsingClocks(PlayerList playerList, Packet<?> packet, Operation<Void> original) {
		MapClocks.broadcast(playerList, (ServerClockManager) (Object) this, packet);
	}
}
