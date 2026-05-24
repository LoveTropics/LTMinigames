package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.criterion.BlockPredicate;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.util.TriState;
import net.minecraft.util.context.ContextMap;

import java.util.Optional;
import java.util.function.Supplier;

public record BlockBreakTrigger(
		Optional<EntityPredicate> predicate,
		Optional<BlockPredicate> blockPredicate,
		GameActionList action) implements IGameBehavior {

	public static final MapCodec<BlockBreakTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			EntityPredicate.CODEC.optionalFieldOf("player_predicate").forGetter(BlockBreakTrigger::predicate),
			BlockPredicate.CODEC.optionalFieldOf("block_predicate").forGetter(BlockBreakTrigger::blockPredicate),
			GameActionList.MAP_CODEC.forGetter(BlockBreakTrigger::action)
	).apply(i, BlockBreakTrigger::new));

	@Override
	public void register(final IGamePhase game, final EventRegistrar events) {
		action.register(game, events);

		events.listen(GamePlayerEvents.BREAK_BLOCK, (player, pos, state, hand) -> {
			if (predicate.isPresent() && !predicate.get().matches(player, player)) {
				return TriState.DEFAULT;
			}
			if (blockPredicate.isPresent() && !blockPredicate.get().matches(player.level(), pos)) {
				return TriState.DEFAULT;
			}
			action.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player));
			return TriState.DEFAULT;
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.BLOCK_BREAK;
	}
}
