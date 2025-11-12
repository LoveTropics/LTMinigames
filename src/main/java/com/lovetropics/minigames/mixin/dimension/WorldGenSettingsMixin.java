package com.lovetropics.minigames.mixin.dimension;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.dimension.RuntimeDimensions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldGenSettings.class)
public class WorldGenSettingsMixin {
	@Unique
	private static final Codec<ResourceKey<Level>> KEY_CODEC = ResourceKey.codec(Registries.DIMENSION);

	@Inject(method = "encode(Lcom/mojang/serialization/DynamicOps;Lnet/minecraft/world/level/levelgen/WorldOptions;Lnet/minecraft/world/level/levelgen/WorldDimensions;)Lcom/mojang/serialization/DataResult;", at = @At("RETURN"), cancellable = true)
	private static <T> void encode(DynamicOps<T> ops, WorldOptions options, WorldDimensions dimensions, CallbackInfoReturnable<DataResult<T>> cir) {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		RuntimeDimensions runtimeDimensions = server != null ? RuntimeDimensions.getOrNull(server) : null;
		if (runtimeDimensions == null) {
			return;
		}
		cir.setReturnValue(cir.getReturnValue().map(tag -> ops.update(tag, "dimensions", dimensionsTag ->
				removeTemporaryDimensions(ops, dimensionsTag, runtimeDimensions)
		)));
	}

	@Unique
	private static <T> T removeTemporaryDimensions(DynamicOps<T> ops, T tag, RuntimeDimensions runtimeDimensions) {
		return ops.getMap(tag).result().map(map ->
				ops.createMap(map.entries().filter(entry -> !isTemporaryDimension(ops, entry.getFirst(), runtimeDimensions)))
		).orElse(tag);
	}

	@Unique
	private static <T> boolean isTemporaryDimension(DynamicOps<T> ops, T key, RuntimeDimensions runtimeDimensions) {
		return KEY_CODEC.parse(ops, key).result().filter(dimension -> runtimeDimensions.isTemporaryDimension(dimension) || looksLikeTemporaryDimension(dimension)).isPresent();
	}

	// TODO: Remove this, we're just cleaning up old data
	@Unique
	private static boolean looksLikeTemporaryDimension(ResourceKey<Level> dimension) {
		return dimension.location().getNamespace().equals(LoveTropics.ID) && dimension.location().getPath().startsWith("tmp_");
	}
}
