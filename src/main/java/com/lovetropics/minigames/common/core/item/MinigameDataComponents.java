package com.lovetropics.minigames.common.core.item;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.diguise.DisguiseType;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MinigameDataComponents {
	public static final DeferredRegister.DataComponents REGISTER = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, LoveTropics.ID);

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<DisguiseType>> DISGUISE = REGISTER.registerComponentType(
			"disguise",
			builder -> builder.persistent(DisguiseType.CODEC).networkSynchronized(DisguiseType.STREAM_CODEC)
	);

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<DisguiseType.EntityConfig>> ENTITY = REGISTER.registerComponentType(
			"entity",
			builder -> builder.persistent(DisguiseType.EntityConfig.CODEC).networkSynchronized(DisguiseType.EntityConfig.STREAM_CODEC)
	);
	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> SIZE = REGISTER.registerComponentType(
			"size",
			builder -> builder.persistent(ExtraCodecs.POSITIVE_FLOAT).networkSynchronized(ByteBufCodecs.FLOAT)
	);

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> SELECTOR = REGISTER.registerComponentType(
			"selector",
			builder -> builder.persistent(Codec.STRING)
	);
}
