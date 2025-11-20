package com.lovetropics.minigames.mixin.event;

import com.lovetropics.minigames.common.core.game.behavior.event.GameWorldEvents;
import com.lovetropics.minigames.common.core.game.impl.GamePhaseManager;
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

	@Inject(method = "neighborChanged", at=@At("HEAD"), cancellable = true)
	public void onNeighbourChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, Orientation orientation, boolean movedByPiston, CallbackInfo ci) {
		if(!level.isClientSide){
			if(GamePhaseManager.get().getGamePhaseAt(level, pos) != null){
				TriState result = GamePhaseManager.get().getGamePhaseAt(level, pos).invoker(GameWorldEvents.TRAPDOOR_TOGGLE)
						.onTrapDoorToggle((ServerLevel) level, pos, state);
				if(result.isFalse()){
					ci.cancel();
				}
			}
		}
	}
}
