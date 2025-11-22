package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public record CycleByEntityAction(
		List<GameActionList> actions
) implements IGameBehavior {
	public static final MapCodec<CycleByEntityAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			GameActionList.CODEC.listOf().fieldOf("actions_by_entity").forGetter(CycleByEntityAction::actions)
	).apply(i, CycleByEntityAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		Object2IntMap<UUID> assignedIndices = new Object2IntOpenHashMap<>();
		AtomicInteger nextIndex = new AtomicInteger();

		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			boolean applied = false;
			List<Entity> entities = targets.asEntities(game);
			for (Entity entity : entities) {
				int assignedIndex = assignedIndices.computeIfAbsent(entity.getUUID(), key -> nextIndex.getAndIncrement() % actions.size());
				applied |= actions.get(assignedIndex).apply(game, context, ActionSubjects.ofEntity(entity));
			}
			return applied;
		});
	}
}
