package com.lovetropics.minigames.mixin.dimension;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.lovetropics.minigames.common.core.map.MapWorldInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// patches for server map level maps
@Mixin(ServerLevel.class)
public class MapServerLevelMixin {
	@Shadow
	@Final
	private ServerLevelData serverLevelData;

	@ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;prepareWeather(Lnet/minecraft/world/level/saveddata/WeatherData;)V"))
	private WeatherData prepareMapWeather(WeatherData weatherData) {
		return serverLevelData instanceof MapWorldInfo mapInfo ? mapInfo.getWeatherData() : weatherData;
	}

	@Inject(method = "getWeatherData", at = @At("HEAD"), cancellable = true)
	private void getMapWeatherData(CallbackInfoReturnable<WeatherData> cir) {
		if (serverLevelData instanceof MapWorldInfo mapInfo) {
			cir.setReturnValue(mapInfo.getWeatherData());
		}
	}

	@Inject(method = "getGameRules", at = @At("HEAD"), cancellable = true)
	private void getMapGameRules(CallbackInfoReturnable<GameRules> cir) {
		if (serverLevelData instanceof MapWorldInfo mapInfo) {
			cir.setReturnValue(mapInfo.getGameRules());
		}
	}

	@Inject(method = "clockManager()Lnet/minecraft/world/clock/ServerClockManager;", at = @At("HEAD"), cancellable = true)
	private void getMapClockManager(CallbackInfoReturnable<ServerClockManager> cir) {
		if (serverLevelData instanceof MapWorldInfo mapInfo) {
			cir.setReturnValue(mapInfo.getOrCreateClockManager((ServerLevel) (Object) this));
		}
	}

	// eepy players change local map time and don't wake up everybody on the server >:(
	@WrapOperation(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;clockManager()Lnet/minecraft/world/clock/ServerClockManager;"))
	private ServerClockManager useLevelClocksOnWakeUp(MinecraftServer server, Operation<ServerClockManager> original) {
		return ((ServerLevel) (Object) this).clockManager();
	}
}
