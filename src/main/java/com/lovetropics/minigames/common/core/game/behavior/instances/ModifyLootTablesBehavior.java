package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameWorldEvents;
import com.lovetropics.minigames.common.util.Codecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record ModifyLootTablesBehavior(
		Map<ResourceKey<LootTable>, Holder<LootTable>> replace,
		Set<ResourceKey<LootTable>> remove
) implements IGameBehavior {
	public static final MapCodec<ModifyLootTablesBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.unboundedMap(LootTable.KEY_CODEC, Codecs.LOOT_TABLE).fieldOf("replace").forGetter(ModifyLootTablesBehavior::replace),
			LootTable.KEY_CODEC.listOf().xmap(Set::copyOf, List::copyOf).optionalFieldOf("remove", Set.of()).forGetter(ModifyLootTablesBehavior::remove)
	).apply(i, ModifyLootTablesBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameWorldEvents.MODIFY_LOOT_TABLE, (generatedLoot, context) -> {
			ResourceKey<LootTable> tableId = ResourceKey.create(Registries.LOOT_TABLE, context.getQueriedLootTableId());
			if (remove.contains(tableId)) {
				generatedLoot.clear();
				return generatedLoot;
			}
			Holder<LootTable> newTable = replace.get(tableId);
			if (newTable == null) {
				return generatedLoot;
			}
			generatedLoot.clear();
			newTable.value().getRandomItemsRaw(context, LootTable.createStackSplitter(context.getLevel(), generatedLoot::add));
			return generatedLoot;
		});
	}
}
