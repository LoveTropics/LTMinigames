package org.lovetropics.games.common.core.game.behavior.instances.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameWorldEvents;
import org.lovetropics.games.common.core.game.region.RegionQuery;

import java.util.Map;
import java.util.Optional;

public record FillContainerInRegionWithLootTableBehaviour(
		RegionQuery region,
		Optional<ResourceKey<LootTable>> lootTable,
		Map<Block, ResourceKey<LootTable>> blockLootTables
) implements IGameBehavior {
	public static final MapCodec<FillContainerInRegionWithLootTableBehaviour> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegionQuery.CODEC.fieldOf("region").forGetter(FillContainerInRegionWithLootTableBehaviour::region),
			ResourceKey.codec(Registries.LOOT_TABLE).optionalFieldOf("loot_table").forGetter(FillContainerInRegionWithLootTableBehaviour::lootTable),
			Codec.unboundedMap(BuiltInRegistries.BLOCK.byNameCodec(), ResourceKey.codec(Registries.LOOT_TABLE)).optionalFieldOf("block_loot_tables", Map.of()).forGetter(FillContainerInRegionWithLootTableBehaviour::blockLootTables)
	).apply(i, FillContainerInRegionWithLootTableBehaviour::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		ResourceKey<LootTable> defaultLootTable = this.lootTable.orElse(null);
		events.listen(GameWorldEvents.CHUNK_LOAD, (level, chunk) -> {
			for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
				if (blockEntity instanceof RandomizableContainer container && region.contains(game, level, blockEntity.getBlockPos())) {
					ResourceKey<LootTable> lootTable = blockLootTables.getOrDefault(blockEntity.getBlockState().getBlock(), defaultLootTable);
					if (lootTable != null) {
						container.setLootTable(lootTable, game.random().nextLong());
					}
				}
			}
		});
	}
}
