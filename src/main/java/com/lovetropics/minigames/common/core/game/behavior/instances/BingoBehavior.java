package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.SoundRegistry;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.behavior.instances.action.PlaySoundAction;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.slf4j.Logger;

import java.util.BitSet;
import java.util.List;
import java.util.Optional;

public record BingoBehavior(
		List<List<ItemPredicate>> grid
) implements IGameBehavior {
	private static final Codec<ItemPredicate> ITEM_OR_PREDICATE_CODEC = Codec.withAlternative(
			ItemPredicate.CODEC,
			ItemStackTemplate.CODEC,
			itemStack -> new ItemPredicate(
					Optional.of(HolderSet.direct(itemStack.typeHolder())),
					MinMaxBounds.Ints.atLeast(itemStack.count()),
					DataComponentMatchers.Builder.components()
							.exact(DataComponentExactPredicate.allOf(itemStack.components().split().added()))
							.build()
			)
	);

	public static final MapCodec<BingoBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ITEM_OR_PREDICATE_CODEC.listOf().listOf().fieldOf("grid").forGetter(BingoBehavior::grid)
	).apply(i, BingoBehavior::new));

	private static final Logger LOGGER = LogUtils.getLogger();

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		BitSet marked = new BitSet();

		events.listen(GamePlayerEvents.CRAFT, (player, crafted, craftingContainer) ->
				tryMarkOff(crafted, marked, player)
		);

		events.listen(GamePlayerEvents.TICK, player -> {
			CraftingContainer craftSlots = player.inventoryMenu.getCraftSlots();
			player.getInventory().clearOrCountMatchingItems(itemStack -> {
				tryMarkOff(itemStack, marked, player);
				return false;
			}, 0, craftSlots);
		});
	}

	private void tryMarkOff(ItemStack itemStack, BitSet marked, ServerPlayer player) {
		int size = grid.size();
		for (int row = 0; row < size; row++) {
			for (int column = 0; column < size; column++) {
				int index = row * size + column;
				if (marked.get(index)) {
					continue;
				}
				ItemPredicate predicate = grid.get(row).get(column);
				if (predicate.test(itemStack)) {
					marked.set(index);
					PlaySoundAction.playToPlayer(player, SoundRegistry.CORRECT.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
					PlayerSet.of(player).showTitle(Component.literal("Well done!").withStyle(ChatFormatting.GOLD), Component.literal("You marked off ").append(itemStack.getHoverName()).withStyle(ChatFormatting.AQUA), 20, 40, 20);
					LOGGER.info("Bingo marked off item {}.{} ({})", row, column, itemStack.getHoverName().getString());
				}
			}
		}
	}
}
