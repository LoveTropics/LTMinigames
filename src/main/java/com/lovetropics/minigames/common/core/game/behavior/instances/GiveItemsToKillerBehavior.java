package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.util.Util;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

public record GiveItemsToKillerBehavior(List<ItemPredicate> predicates) implements IGameBehavior {
	public static final MapCodec<GiveItemsToKillerBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ExtraCodecs.compactListCodec(ItemPredicate.CODEC).fieldOf("item_predicate").forGetter(GiveItemsToKillerBehavior::predicates)
	).apply(i, GiveItemsToKillerBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GamePlayerEvents.DEATH, (player, source) -> {
			final ServerPlayer killer = Util.getKillerPlayer(player, source);
			if (killer != null && game.participants().contains(killer)) {
				giveItems(player, killer);
			}
			return TriState.DEFAULT;
		});
	}

	private void giveItems(final ServerPlayer player, final ServerPlayer killer) {
		final Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (matches(inventory.getItem(i))) {
				killer.getInventory().placeItemBackInInventory(inventory.removeItemNoUpdate(i));
			}
		}
	}

	private boolean matches(final ItemStack item) {
		for (final ItemPredicate predicate : predicates) {
			if (predicate.test(item)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.GIVE_ITEMS_TO_KILLER;
	}
}
