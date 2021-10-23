package com.lovetropics.minigames.common.content.mangroves_and_pianguas.plot.plant;

import com.lovetropics.minigames.common.content.mangroves_and_pianguas.plot.plant.state.PlantState;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.Random;

public final class Plant {
	private final PlantType type;
	private final PlantCoverage functionalCoverage;
	@Nullable
	private final PlantCoverage decorationCoverage;
	private final PlantState state;

	private final PlantCoverage coverage;

	public Plant(PlantType type, PlantCoverage functionalCoverage, @Nullable PlantCoverage decorationCoverage) {
		this(type, functionalCoverage, decorationCoverage, new PlantState());
	}

	private Plant(PlantType type, PlantCoverage functionalCoverage, @Nullable PlantCoverage decorationCoverage, PlantState state) {
		this.type = type;
		this.functionalCoverage = functionalCoverage;
		this.decorationCoverage = decorationCoverage;
		this.state = state;

		this.coverage = decorationCoverage != null ? PlantCoverage.or(functionalCoverage, decorationCoverage) : functionalCoverage;
	}

	public PlantType type() {
		return type;
	}

	public PlantCoverage functionalCoverage() {
		return functionalCoverage;
	}

	@Nullable
	public PlantCoverage decorationCoverage() {
		return decorationCoverage;
	}

	public PlantCoverage coverage() {
		return coverage;
	}

	public PlantState state() {
		return state;
	}

	@Nullable
	public <S> S state(PlantState.Key<S> key) {
		return state.get(key);
	}

	public void spawnPoof(ServerWorld world) {
		spawnPoof(world, 20, 0.15);
	}

	public void spawnPoof(ServerWorld world, int count, double speed) {
		Random random = world.rand;

		for (BlockPos pos : this.coverage) {
			for (int i = 0; i < count; i++) {
				double vx = random.nextGaussian() * 0.02;
				double vy = random.nextGaussian() * 0.02;
				double vz = random.nextGaussian() * 0.02;
				world.spawnParticle(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1, vx, vy, vz, speed);
			}
		}
	}
}
