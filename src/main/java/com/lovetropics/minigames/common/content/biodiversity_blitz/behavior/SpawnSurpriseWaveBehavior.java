package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior;

import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.BbMobSpawner;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.impl.BbCreeperEntity;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.PlotsState;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

public class SpawnSurpriseWaveBehavior implements IGameBehavior {
	public static final MapCodec<SpawnSurpriseWaveBehavior> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.INT.fieldOf("wave_size").forGetter(b -> b.waveSize)
	).apply(instance, SpawnSurpriseWaveBehavior::new));
	private final int waveSize;

	public SpawnSurpriseWaveBehavior(int waveSize) {
		this.waveSize = waveSize;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		PlotsState plots = game.state().getOrThrow(PlotsState.KEY);

		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			ServerLevel world = game.level();

			for (Plot plot : plots) {
				BbMobSpawner.spawnWaveEntities(world, world.getRandom(),
						plot, waveSize, 0, SpawnSurpriseWaveBehavior::selectEntityForWave,
						(entities, random, w, plot1, waveIndex) -> {
						});
			}

			return true;
		});
	}

	private static Mob selectEntityForWave(RandomSource random, Level level, Plot plot, int plotIndex, int waveIndex) {
		return new BbCreeperEntity(EntityType.CREEPER, level, plot);
	}
}
