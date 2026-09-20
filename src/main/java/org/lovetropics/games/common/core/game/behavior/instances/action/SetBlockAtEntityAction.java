package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.lovetropics.lib.codec.MoreCodecs;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public record SetBlockAtEntityAction(BlockStateProvider block) implements IGameBehavior {
	public static final MapCodec<SetBlockAtEntityAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			MoreCodecs.BLOCK_STATE_PROVIDER.fieldOf("block").forGetter(c -> c.block)
	).apply(i, SetBlockAtEntityAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.applyToEntities(game, (context, level, entity) -> {
			BlockPos pos = entity.blockPosition();
			BlockState state = block.getState(level, game.random(), pos);
			level.setBlockAndUpdate(pos, state);
			return true;
		});
	}
}
