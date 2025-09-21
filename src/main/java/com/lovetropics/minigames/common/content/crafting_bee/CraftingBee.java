package com.lovetropics.minigames.common.content.crafting_bee;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.util.registry.GameBehaviorEntry;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

import java.util.function.Supplier;

public class CraftingBee {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameBehaviorEntry<CraftingBeeBehavior> CRAFTING_BEE = REGISTRATE.object("crafting_bee")
			.behavior(CraftingBeeBehavior.CODEC)
			.register();

	public static final Supplier<DataComponentType<CraftedUsing>> CRAFTED_USING = REGISTRATE.simple(
			"crafted_using",
			Registries.DATA_COMPONENT_TYPE,
			() -> DataComponentType.<CraftedUsing>builder()
					.networkSynchronized(CraftedUsing.STREAM_CODEC)
					.persistent(CraftedUsing.CODEC)
					.cacheEncoding()
					.build()
	);

	public static void init() {
	}
}
