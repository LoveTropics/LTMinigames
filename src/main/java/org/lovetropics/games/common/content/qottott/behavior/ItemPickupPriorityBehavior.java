package org.lovetropics.games.common.content.qottott.behavior;

import org.lovetropics.games.common.content.qottott.Qottott;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.behavior.event.PickUpResult;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.function.Supplier;

public record ItemPickupPriorityBehavior(Optional<ItemPredicate> itemPredicate, float maxSeconds) implements IGameBehavior {
	public static final MapCodec<ItemPickupPriorityBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ItemPredicate.CODEC.optionalFieldOf("item").forGetter(ItemPickupPriorityBehavior::itemPredicate),
			Codec.FLOAT.fieldOf("max_seconds").forGetter(ItemPickupPriorityBehavior::maxSeconds)
	).apply(i, ItemPickupPriorityBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GamePlayerEvents.PICK_UP_ITEM, (player, item) -> {
			ItemStack stack = item.getItem();
			if (itemPredicate.isEmpty() || itemPredicate.get().test(stack)) {
				float minSeconds = Math.max(maxSeconds - getPickupPriority(player), 0.0f);
				int minAge = Mth.floor(minSeconds * SharedConstants.TICKS_PER_SECOND);
				if (item.getAge() <= minAge) {
					return PickUpResult.CANCEL;
				}
			}
			return PickUpResult.PASS;
		});
	}

	private static float getPickupPriority(ServerPlayer player) {
		AttributeInstance attribute = player.getAttribute(Qottott.PICKUP_PRIORITY);
		return attribute != null ? (float) attribute.getValue() : 0.0f;
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return Qottott.PICKUP_PRIORITY_BEHAVIOR;
	}
}
