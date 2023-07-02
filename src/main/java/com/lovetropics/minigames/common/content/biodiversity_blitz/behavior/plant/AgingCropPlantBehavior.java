package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class AgingCropPlantBehavior extends AgingPlantBehavior {
	public static final Codec<AgingCropPlantBehavior> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("interval").forGetter(c -> c.interval)
	).apply(i, AgingCropPlantBehavior::new));

	public AgingCropPlantBehavior(int interval) {
		super(interval);
	}

	@Override
	protected BlockState ageUp(RandomSource random, BlockState state) {
		// Skip 50% of crops this tick
		if (random.nextInt(2) == 0) {
			return state;
		}

		if (state.hasProperty(BlockStateProperties.AGE_3)) {
			int age = state.getValue(BlockStateProperties.AGE_3);
			if (age < 3) {
				return state.setValue(BlockStateProperties.AGE_3, age + 1);
			}
		} else if (state.hasProperty(BlockStateProperties.AGE_7)) {
			int age = state.getValue(BlockStateProperties.AGE_7);
			if (age < 7) {
				return state.setValue(BlockStateProperties.AGE_7, age + 1);
			}
		}

		return state;
	}
}
