package com.lovetropics.minigames.common.core.item;

import com.lovetropics.minigames.LoveTropics;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MinigameDataComponents {
	public static final DeferredRegister.DataComponents REGISTER = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, LoveTropics.ID);

	/****************************** REGISTER COMPONENTS BELOW ******************************/

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> SELECTOR = REGISTER.registerComponentType(
			"selector",
			builder -> builder.persistent(Codec.STRING)
	);

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> POWERUP = REGISTER.registerComponentType(
			"powerup",
			builder -> builder.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC).cacheEncoding()
	);
}
