package org.lovetropics.dimensions.mixin.clock;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;
import org.lovetropics.dimensions.SharedDimensionState;
import org.lovetropics.dimensions.duck.SharedDimensionStateBindable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.clock.ServerClockManager$ClockInstance")
public class ServerClockInstanceMixin implements SharedDimensionStateBindable {
	@Unique
	private @Nullable SharedDimensionState ltdimensions$sharedState;

	@Override
	public void ltdimensions$bindTo(SharedDimensionState sharedState) {
		ltdimensions$sharedState = sharedState;
	}

	@WrapOperation(method = "packNetworkState", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getGlobalGameRules()Lnet/minecraft/world/level/gamerules/GameRules;"))
	private GameRules useLevelGameRules(MinecraftServer server, Operation<GameRules> original) {
		return ltdimensions$sharedState != null ? ltdimensions$sharedState.gameRules() : original.call(server);
	}
}
