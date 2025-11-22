package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class BerriesPlantBehavior extends AgingPlantBehavior {
	public static final MapCodec<BerriesPlantBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.fieldOf("interval").forGetter(c -> c.interval)
	).apply(i, BerriesPlantBehavior::new));

	public BerriesPlantBehavior(int interval) {
		super(interval);
	}

	@Override
	protected BlockState ageUp(RandomSource random, BlockState state) {
		if (state.hasProperty(BlockStateProperties.AGE_3)) {
			int age = state.getValue(BlockStateProperties.AGE_3);
			if (age < 1 || age < 3 && random.nextInt(24) == 0) {
				return state.setValue(BlockStateProperties.AGE_3, age + 1);
			}
		}

		return state;
	}
}
