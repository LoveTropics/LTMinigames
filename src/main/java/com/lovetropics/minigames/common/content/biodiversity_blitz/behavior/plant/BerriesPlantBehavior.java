package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.state.properties.BlockStateProperties;

import java.util.Random;

public final class BerriesPlantBehavior extends AgingPlantBehavior {
	public static final Codec<BerriesPlantBehavior> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("interval").forGetter(c -> c.interval)
	).apply(instance, BerriesPlantBehavior::new));

	public BerriesPlantBehavior(int interval) {
		super(interval);
	}

	@Override
	protected BlockState ageUp(Random random, BlockState state) {
		int age = state.get(BlockStateProperties.AGE_0_3);
		if (age < 1 || age < 3 && random.nextInt(128) == 0) {
			return state.with(BlockStateProperties.AGE_0_3, age + 1);
		}

		return state;
	}
}