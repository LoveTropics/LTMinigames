package com.lovetropics.minigames.common.core.game.behavior.instances.tweak;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GameWorldEvents;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.criterion.BlockPredicate;
import net.minecraft.util.TriState;

import java.util.List;

public record PreventBreakBehavior(List<BlockPredicate> predicates) implements IGameBehavior {
	public static final MapCodec<PreventBreakBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BlockPredicate.CODEC.listOf().fieldOf("predicates").forGetter(c -> c.predicates)
	).apply(i, PreventBreakBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GamePlayerEvents.BREAK_BLOCK, (player, pos, state, hand) -> {
			for (BlockPredicate predicate : predicates) {
				if (predicate.matches(player.level(), pos)) {
					return TriState.FALSE;
				}
			}
			return TriState.DEFAULT;
		});
		events.listen(GameWorldEvents.EXPLOSION_DETONATE, (explosion, affectedBlocks, affectedEntities) -> {
			affectedBlocks.removeIf(pos -> {
				for (BlockPredicate predicate : predicates) {
					if (predicate.matches(game.level(), pos)) {
						return true;
					}
				}
				return false;
			});
		});
	}
}
