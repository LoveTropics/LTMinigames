package com.lovetropics.minigames.common.core.game.behavior.instances.tweak;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public record DisableCraftingBehavior(
		Optional<ItemPredicate> item
) implements IGameBehavior {
	public static final MapCodec<DisableCraftingBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ItemPredicate.CODEC.optionalFieldOf("item").forGetter(DisableCraftingBehavior::item)
	).apply(i, DisableCraftingBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GamePlayerEvents.CRAFT_RESULT, (player, crafted, input, recipe) -> {
			if (item.isEmpty() || item.get().test(crafted)) {
				return ItemStack.EMPTY;
			}
			return crafted;
		});
	}
}
