package org.lovetropics.games.common.core.game.behavior.instances.tweak;

import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameWorldEvents;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.BlockPredicate;

import java.util.Optional;

public record DisableTntDestructionBehavior(
		Optional<BlockPredicate> blockPredicate
) implements IGameBehavior {
	public static final MapCodec<DisableTntDestructionBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BlockPredicate.CODEC.optionalFieldOf("block_predicate").forGetter(DisableTntDestructionBehavior::blockPredicate)
	).apply(i, DisableTntDestructionBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		if (blockPredicate.isEmpty()) {
			events.listen(GameWorldEvents.EXPLOSION_DETONATE, (_, _, affectedBlocks, _) -> affectedBlocks.clear());
		} else {
			BlockPredicate blockPredicate = this.blockPredicate.get();
			events.listen(GameWorldEvents.EXPLOSION_DETONATE, (level, _, affectedBlocks, _) ->
					affectedBlocks.removeIf(pos -> blockPredicate.matches(level, pos))
			);
		}
	}
}
