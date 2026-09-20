package org.lovetropics.games.mixin.dimension;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.lovetropics.games.common.core.dimension.RuntimeDimensions;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Function;

// We don't normally mark WorldGenSettings as dirty when runtime dimensions change (which is good), but if we are to save for any reason - we definitely don't want to store the dimension that is supposed to be transient
@Mixin(WorldGenSettings.class)
public class WorldGenSettingsMixin {
	@Unique
	private static final Codec<ResourceKey<Level>> KEY_CODEC = ResourceKey.codec(Registries.DIMENSION);

	@WrapOperation(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/codecs/RecordCodecBuilder;create(Ljava/util/function/Function;)Lcom/mojang/serialization/Codec;"))
	private static Codec<WorldGenSettings> init(Function<RecordCodecBuilder.Instance<WorldGenSettings>, ? extends App<RecordCodecBuilder.Mu<WorldGenSettings>, WorldGenSettings>> builder, Operation<Codec<WorldGenSettings>> original) {
		Codec<WorldGenSettings> oldCodec = original.call(builder);
		return new Codec<>() {
			@Override
			public <T> DataResult<Pair<WorldGenSettings, T>> decode(DynamicOps<T> ops, T input) {
				return oldCodec.decode(ops, input);
			}

			@Override
			public <T> DataResult<T> encode(WorldGenSettings input, DynamicOps<T> ops, T prefix) {
				return oldCodec.encode(input, ops, prefix).map(encoded ->
						ltminigames$removeTemporaryDimensions(ops, encoded)
				);
			}
		};
	}

	@Unique
	private static <T> T ltminigames$removeTemporaryDimensions(DynamicOps<T> ops, T root) {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		RuntimeDimensions runtimeDimensions = server != null ? RuntimeDimensions.getOrNull(server) : null;
		if (runtimeDimensions == null) {
			return root;
		}
		return ops.update(root, "dimensions", dimensionsTag ->
				ltminigames$removeTemporaryDimensions(ops, dimensionsTag, runtimeDimensions)
		);
	}

	@Unique
	private static <T> T ltminigames$removeTemporaryDimensions(DynamicOps<T> ops, T tag, RuntimeDimensions runtimeDimensions) {
		return ops.getMap(tag).result().map(map ->
				ops.createMap(map.entries().filter(entry -> !ltminigames$isTemporaryDimension(ops, entry.getFirst(), runtimeDimensions)))
		).orElse(tag);
	}

	@Unique
	private static <T> boolean ltminigames$isTemporaryDimension(DynamicOps<T> ops, T key, RuntimeDimensions runtimeDimensions) {
		return KEY_CODEC.parse(ops, key).result().filter(runtimeDimensions::isTemporaryDimension).isPresent();
	}
}
