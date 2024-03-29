package com.lovetropics.minigames.common.core.game.map;

import com.lovetropics.lib.codec.CodecRegistry;
import com.lovetropics.minigames.common.util.Util;
import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public final class GameMapProviders {
	public static final CodecRegistry<ResourceLocation, Codec<? extends IGameMapProvider>> REGISTRY = CodecRegistry.resourceLocationKeys();

	public static final Codec<IGameMapProvider> CODEC = REGISTRY.dispatchStable(IGameMapProvider::getCodec, Function.identity());

	private static void register(final String name, final Codec<? extends IGameMapProvider> codec) {
		REGISTRY.register(Util.resource(name), codec);
	}

	static {
		register("load_map", LoadMapProvider.CODEC);
		register("random", RandomMapProvider.CODEC);
		register("inline", InlineMapProvider.CODEC);
		register("void", VoidMapProvider.CODEC);
	}
}
