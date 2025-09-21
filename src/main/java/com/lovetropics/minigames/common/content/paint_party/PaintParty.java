package com.lovetropics.minigames.common.content.paint_party;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.paint_party.entity.PaintBallEntity;
import com.lovetropics.minigames.common.util.registry.GameBehaviorEntry;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import static net.minecraft.world.level.storage.loot.LootTable.lootTable;

public class PaintParty {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameBehaviorEntry<PaintPartyBehaviour> BEHAVIOR = REGISTRATE.object("paint_party")
			.behavior(PaintPartyBehaviour.CODEC)
			.register();

	public static final RegistryEntry<EntityType<?>, EntityType<PaintBallEntity>> PAINTBALL = REGISTRATE.entity("paintball", (EntityType.EntityFactory<PaintBallEntity>) PaintBallEntity::new, MobCategory.MISC)
			.properties(properties -> properties.sized(0.25F, 0.25F).setShouldReceiveVelocityUpdates(true).setUpdateInterval(3))
			.loot((loot, type) -> loot.add(type, lootTable()))
			.renderer(() -> PaintParty::createPaintBallRenderer)
			.register();

	public static void init() {
	}

	private static ThrownItemRenderer<PaintBallEntity> createPaintBallRenderer(EntityRendererProvider.Context context) {
		return new ThrownItemRenderer<>(context, 1.0f, false);
	}
}
