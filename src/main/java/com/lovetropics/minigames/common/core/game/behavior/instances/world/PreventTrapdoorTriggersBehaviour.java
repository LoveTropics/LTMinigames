package com.lovetropics.minigames.common.core.game.behavior.instances.world;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameWorldEvents;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.util.TriState;

import java.util.Optional;

public record PreventTrapdoorTriggersBehaviour(
		Optional<BlockPredicate> blockPredicate
) implements IGameBehavior {
	public static final MapCodec<PreventTrapdoorTriggersBehaviour> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BlockPredicate.CODEC.optionalFieldOf("block_predicate").forGetter(PreventTrapdoorTriggersBehaviour::blockPredicate)
	).apply(i, PreventTrapdoorTriggersBehaviour::new));
	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameWorldEvents.TRAPDOOR_TOGGLE, (level, pos, state) -> {
			if(blockPredicate.isPresent()) {
				if(blockPredicate.get().matches(level, pos)){
					return TriState.FALSE;
				} else {
					return TriState.TRUE;
				}
			}
			return TriState.FALSE;
		});
	}
}
