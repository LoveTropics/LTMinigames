package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContext;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.util.registry.GameBehaviorEntry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.BlockPos;

import java.util.Iterator;
import java.util.function.Supplier;

public record BlockInRegionTrigger(
		String region,
		BlockPredicate predicate,
		boolean allMatch,
		GameActionList<Void> actions
) implements IGameBehavior {

	public static final MapCodec<BlockInRegionTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("region").forGetter(BlockInRegionTrigger::region),
			BlockPredicate.CODEC.fieldOf("predicate").forGetter(BlockInRegionTrigger::predicate),
			Codec.BOOL.optionalFieldOf("all_match", true).forGetter(BlockInRegionTrigger::allMatch),
			GameActionList.VOID_CODEC.fieldOf("actions").forGetter(BlockInRegionTrigger::actions)
	).apply(i, BlockInRegionTrigger::new));


	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		BlockBox blockRegion = game.mapRegions().getOrThrow(region);

		events.listen(GamePhaseEvents.TICK, () -> {
			for (BlockPos pos : blockRegion) {
				boolean matches = predicate.matches(game.level(), pos);
				if (allMatch && !matches) {
					return;
				} else if (!allMatch && matches) {
					actions.apply(game, GameActionContext.EMPTY);
					return;
				}
			}
			if (allMatch) {
				actions.apply(game, GameActionContext.EMPTY);
			}

		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.BLOCK_IN_REGION;
	}
}
