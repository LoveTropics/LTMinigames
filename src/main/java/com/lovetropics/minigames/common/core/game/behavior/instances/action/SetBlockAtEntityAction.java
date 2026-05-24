package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
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
		events.applyToEntities(game, (context, entity) -> {
			BlockPos pos = entity.blockPosition();
			BlockState state = block.getState(game.level(), entity.level().getRandom(), pos);
			entity.level().setBlockAndUpdate(pos, state);
			return true;
		});
	}
}
