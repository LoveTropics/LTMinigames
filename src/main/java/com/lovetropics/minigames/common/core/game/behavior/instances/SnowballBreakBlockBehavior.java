package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameWorldEvents;
import com.lovetropics.minigames.common.core.game.state.DebugModeState;
import com.lovetropics.minigames.common.core.game.state.GameStateMap;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Supplier;

public final class SnowballBreakBlockBehavior implements IGameBehavior {
	public static final MapCodec<SnowballBreakBlockBehavior> CODEC = MapCodec.unit(SnowballBreakBlockBehavior::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameWorldEvents.PROJECTILE_IMPACT, ((projectile, result) -> {
			if (!(projectile instanceof Snowball snowball)) {
				return;
			}

			if (!(result instanceof BlockHitResult blockHitResult)) {
				return;
			}
			BlockPos blockPos = blockHitResult.getBlockPos();
			BlockState blockState = game.level().getBlockState(blockPos);
			if (blockState.is(Blocks.SNOW_BLOCK)) {
				game.level().setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());
			}
		}));
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.SNOWBALL_BREAK_BLOCK;
	}
}
