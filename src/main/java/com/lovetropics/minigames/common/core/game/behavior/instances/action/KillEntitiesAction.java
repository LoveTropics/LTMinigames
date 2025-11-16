package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public record KillEntitiesAction(
		EntityPredicate predicate,
		Optional<Integer> count
) implements IGameBehavior {
	public static final MapCodec<KillEntitiesAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			EntityPredicate.CODEC.fieldOf("entity_predicate").forGetter(KillEntitiesAction::predicate),
			Codec.INT.optionalFieldOf("count").forGetter(KillEntitiesAction::count)
	).apply(i, KillEntitiesAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			List<Entity> candidates = new ArrayList<>();
			for (Entity entity : game.level().getAllEntities()) {
				if (predicate.matches(game.level(), Vec3.ZERO, entity)) {
					candidates.add(entity);
				}
			}
			int count = this.count.orElse(Integer.MAX_VALUE);
			Util.shuffle(candidates, game.random());
			for (int i = 0; i < candidates.size() && i < count; i++) {
				candidates.get(i).kill(game.level());
			}
			return true;
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.KILL_ENTITIES;
	}
}
