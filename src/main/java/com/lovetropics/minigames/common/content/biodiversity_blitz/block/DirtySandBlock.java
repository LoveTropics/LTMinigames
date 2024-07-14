package com.lovetropics.minigames.common.content.biodiversity_blitz.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.TriState;

public class DirtySandBlock extends Block {
	public DirtySandBlock(Properties properties) {
		super(properties);
	}

	@Override
	public TriState canSustainPlant(BlockState state, BlockGetter level, BlockPos soilPosition, Direction facing, BlockState plant) {
		return TriState.TRUE;
	}
}
