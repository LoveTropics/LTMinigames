package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GameWorldEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public record LimitedTimeLootAction(
		HolderSet<Block> blocks,
		ResourceKey<LootTable> lootTable,
		int durationTicks
) implements IGameBehavior {
	public static final MapCodec<LimitedTimeLootAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("blocks").forGetter(LimitedTimeLootAction::blocks),
			ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("loot_table").forGetter(LimitedTimeLootAction::lootTable),
			Codec.INT.fieldOf("duration_ticks").forGetter(LimitedTimeLootAction::durationTicks)
	).apply(i, LimitedTimeLootAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		Object2LongMap<UUID> expiryTimes = new Object2LongOpenHashMap<>();
		expiryTimes.defaultReturnValue(game.ticks());

		LootTable lootTable = game.server().reloadableRegistries().getLootTable(this.lootTable);
		if (lootTable == LootTable.EMPTY) {
			throw new GameException(Component.literal("No loot table with id: " + this.lootTable));
		}

		events.listen(GameActionEvents.APPLY_TO_PLAYER, (context, player) -> {
			expiryTimes.compute(player.getUUID(), (playerId, lastExpiryTime) -> {
				long baseTime = lastExpiryTime != null ? lastExpiryTime : game.ticks();
				return baseTime + durationTicks;
			});
			return true;
		});

		events.listen(GameWorldEvents.BLOCK_DROPS, (player, pos, blockState, blockEntity, tool, drops) -> {
			ServerLevel level = player.level();
			if (expiryTimes.getLong(player.getUUID()) <= game.ticks() || !blockState.is(blocks)) {
				return;
			}
			LootParams params = new LootParams.Builder(level)
					.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
					.withParameter(LootContextParams.TOOL, tool)
					.withOptionalParameter(LootContextParams.THIS_ENTITY, player)
					.withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity)
					.withParameter(LootContextParams.BLOCK_STATE, blockState)
					.create(LootContextParamSets.BLOCK);
			for (ItemStack item : lootTable.getRandomItems(params)) {
				double x = pos.getX() + 0.5 + Mth.nextDouble(level.random, -0.25, 0.25);
				double y = pos.getY() + 0.5 + Mth.nextDouble(level.random, -0.25, 0.25) - EntityType.ITEM.getHeight() / 2.0;
				double z = pos.getZ() + 0.5 + Mth.nextDouble(level.random, -0.25, 0.25);
				ItemEntity entity = new ItemEntity(level, x, y, z, item);
				entity.setDefaultPickUpDelay();
				drops.add(entity);
			}
		});
	}
}
