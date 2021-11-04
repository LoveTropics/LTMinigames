package com.lovetropics.minigames.common.core.game.behavior.instances.tweak;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameWorldEvents;
import com.lovetropics.minigames.common.util.BlockStatePredicate;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.server.ServerWorld;

public class DisableTntDestructionBehavior implements IGameBehavior {
	public static final Codec<DisableTntDestructionBehavior> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
				BlockStatePredicate.CODEC.optionalFieldOf("block_predicate", BlockStatePredicate.ANY).forGetter(c -> c.blockPredicate)
		).apply(instance, DisableTntDestructionBehavior::new);
	});

	private final BlockStatePredicate blockPredicate;

	public DisableTntDestructionBehavior(BlockStatePredicate blockPredicate) {
		this.blockPredicate = blockPredicate;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameWorldEvents.EXPLOSION_DETONATE, (explosion, affectedBlocks, affectedEntities) -> {
			ServerWorld world = game.getWorld();
			affectedBlocks.removeIf(pos -> blockPredicate.test(world.getBlockState(pos)));
		});
	}
}
