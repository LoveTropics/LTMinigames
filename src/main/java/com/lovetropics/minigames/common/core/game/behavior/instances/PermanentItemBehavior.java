package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.Optional;

public record PermanentItemBehavior(ItemStackTemplate item, int interval, Optional<Integer> maxCount) implements IGameBehavior {
	public static final MapCodec<PermanentItemBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ItemStackTemplate.CODEC.fieldOf("item").forGetter(c -> c.item),
			Codec.INT.optionalFieldOf("interval", 5).forGetter(c -> c.interval),
			Codec.INT.optionalFieldOf("max_count").forGetter(c -> c.maxCount)
	).apply(i, PermanentItemBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GamePlayerEvents.TICK, player -> {
			if (game.participants().contains(player) && game.ticks() % interval == 0) {
				int currentCount = player.getInventory().countItem(item.item().value());
				int targetCount = maxCount.orElse(item.count());
				if (currentCount < targetCount) {
					int dropCount = Math.min(targetCount - currentCount, item.count());
					player.getInventory().add(item.create().copyWithCount(dropCount));
				}
			}
		});
	}
}
