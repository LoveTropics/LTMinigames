package org.lovetropics.games.mixin.event;

import org.lovetropics.games.common.core.game.IGameLookup;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.event.GameWorldEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.TriState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TrapDoorBlock.class)
public class TrapdoorBlockMixin {

	@Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true)
	public void onNeighbourChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, Orientation orientation, boolean movedByPiston, CallbackInfo ci) {
		if (level instanceof ServerLevel serverLevel) {
			IGamePhase phase = IGameLookup.get().getGamePhaseAt(level, pos);
			if (phase != null) {
				TriState result = phase.invoker(GameWorldEvents.TRAPDOOR_TOGGLE).onTrapDoorToggle(serverLevel, pos, state);
				if (result.isFalse()) {
					ci.cancel();
				}
			}
		}
	}
}
