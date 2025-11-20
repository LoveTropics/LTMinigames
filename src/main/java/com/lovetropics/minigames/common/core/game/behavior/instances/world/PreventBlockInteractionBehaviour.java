package com.lovetropics.minigames.common.core.game.behavior.instances.world;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Optional;

public record PreventBlockInteractionBehaviour(
		Optional<BlockPredicate> blockPredicate
) implements IGameBehavior {
	public static final MapCodec<PreventBlockInteractionBehaviour> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BlockPredicate.CODEC.optionalFieldOf("block_predicate").forGetter(PreventBlockInteractionBehaviour::blockPredicate)
	).apply(i, PreventBlockInteractionBehaviour::new));
	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GamePlayerEvents.USE_BLOCK, this::onUseBlock);
		events.listen(GamePlayerEvents.USE_ITEM_ON_BLOCK, this::onUseBlock);
	}

	private InteractionResult onUseBlock(ServerPlayer player, ServerLevel world, BlockPos pos, InteractionHand hand, BlockHitResult hitResult) {
		if(blockPredicate.isPresent()) {
			if(blockPredicate.get().matches(world, pos)){
				return InteractionResult.CONSUME;
			}
		} else {
			return InteractionResult.CONSUME;
		}
		return InteractionResult.PASS;
	}
}
