package com.lovetropics.minigames.common.core.game.predicate.loot;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;
import net.minecraft.core.Holder;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

public class LootItemConditions {

	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();
	public static final Holder<LootItemConditionType> IS_MINIGAME = REGISTRATE.object("is_minigame")
			.lootItemConditionType(() -> new LootItemConditionType(IsMinigameCondition.CODEC)).register();

	public static void init() {
	}
}
