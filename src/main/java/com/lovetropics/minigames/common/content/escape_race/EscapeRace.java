package com.lovetropics.minigames.common.content.escape_race;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.render.entity.DriftwoodRenderer;
import com.lovetropics.minigames.common.content.escape_race.vending_machine.VendingMachineEntity;
import com.lovetropics.minigames.common.content.escape_race.vending_machine.VendingMachineEntityRenderer;
import com.lovetropics.minigames.common.content.survive_the_tide.entity.DriftwoodEntity;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import static net.minecraft.world.level.storage.loot.LootTable.lootTable;

public class EscapeRace {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final RegistryEntry<EntityType<?>, EntityType<VendingMachineEntity>> VENDING_MACHINE = REGISTRATE.entity("vending_machine", VendingMachineEntity::new, MobCategory.MISC)
			.properties(properties -> properties.sized(2.0F, 3.0F).setShouldReceiveVelocityUpdates(true).setUpdateInterval(3))
			.loot((loot, type) -> loot.add(type, lootTable()))
			.renderer(() -> VendingMachineEntityRenderer::new)
			.register();


	public static void init() {
	}
}
