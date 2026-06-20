package com.lovetropics.minigames.common.core.game.map;

import com.lovetropics.lib.codec.CodecRegistry;
import com.lovetropics.minigames.LoveTropics;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;

import java.util.function.Function;

public final class GameMapProviders {
	public static final CodecRegistry<Identifier, MapCodec<? extends IGameMapProvider>> REGISTRY = CodecRegistry.idKeys();

	public static final Codec<IGameMapProvider> CODEC = REGISTRY.dispatchStable(IGameMapProvider::getCodec, Function.identity());

	private static void register(final String name, final MapCodec<? extends IGameMapProvider> codec) {
		REGISTRY.register(LoveTropics.id(name), codec);
	}

	static {
		register("load_map", LoadMapProvider.CODEC);
		register("random", RandomMapProvider.CODEC);
		register("inline", InlineMapProvider.CODEC);
		register("void", VoidMapProvider.CODEC);
		register("generator", GeneratorMapProvider.CODEC);
	}
}
