package com.lovetropics.minigames.mixin.clock;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.lovetropics.minigames.common.core.map.MapClocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.clock.ServerClockManager$ClockInstance")
public class ServerClockInstanceMixin implements MapClocks.Access {
	@Unique
	private @Nullable ServerLevel ltminigames$level;

	@Override
	public void ltminigames$setLevel(ServerLevel level) {
		ltminigames$level = level;
	}

	@Override
	public @Nullable ServerLevel ltminigames$getLevel() {
		return ltminigames$level;
	}

	@WrapOperation(method = "packNetworkState", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getGlobalGameRules()Lnet/minecraft/world/level/gamerules/GameRules;"))
	private GameRules useLevelGameRules(MinecraftServer server, Operation<GameRules> original) {
		return ltminigames$level != null ? ltminigames$level.getGameRules() : original.call(server);
	}
}
