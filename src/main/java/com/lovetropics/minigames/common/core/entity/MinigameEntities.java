package com.lovetropics.minigames.common.core.entity;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;
import com.tterrag.registrate.util.entry.EntityEntry;
import net.minecraft.client.renderer.entity.LightningBoltRenderer;
import net.minecraft.world.entity.MobCategory;

import static net.minecraft.world.level.storage.loot.LootTable.lootTable;

public class MinigameEntities {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final EntityEntry<QuietLightningBolt> QUIET_LIGHTNING_BOLT = REGISTRATE.entity("quiet_lightning_bolt", QuietLightningBolt::new, MobCategory.MISC)
			.properties(properties -> properties.noSave().sized(0.0F, 0.0F).clientTrackingRange(16).updateInterval(Integer.MAX_VALUE))
			.loot((loot, type) -> loot.add(type, lootTable()))
			.renderer(() -> LightningBoltRenderer::new)
			.register();

	public static void init() {
	}
}
