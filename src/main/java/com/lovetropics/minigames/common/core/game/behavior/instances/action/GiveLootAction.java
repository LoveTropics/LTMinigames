package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.util.Util;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public record GiveLootAction(ResourceKey<LootTable> lootTable) implements IGameBehavior {
	public static final MapCodec<GiveLootAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("loot_table").forGetter(c -> c.lootTable)
	).apply(i, GiveLootAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameActionEvents.APPLY_TO_PLAYER, (context, player) -> addLootTableToInventory(player));
	}

	private boolean addLootTableToInventory(final ServerPlayer player) {
		LootParams params = new LootParams.Builder(player.level())
				.withParameter(LootContextParams.THIS_ENTITY, player)
				.withParameter(LootContextParams.ORIGIN, player.position())
				.withLuck(player.getLuck())
				.create(LootContextParamSets.GIFT);

		boolean changed = false;
		for (ItemStack stack : player.level().getServer().reloadableRegistries().getLootTable(lootTable).getRandomItems(params)) {
			if (Util.addItemStackToInventory(player, stack)) {
				changed = true;
			}
		}

		if (changed) {
			player.inventoryMenu.broadcastChanges();
			return true;
		} else {
			return false;
		}
	}
}
