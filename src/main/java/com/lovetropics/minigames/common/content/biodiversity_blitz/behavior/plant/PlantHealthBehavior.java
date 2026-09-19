package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant;

import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbPlantEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.Plant;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.state.PlantHealth;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.state.PlantNotPathfindable;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public record PlantHealthBehavior(int health, boolean notPathfindable) implements IGameBehavior {
	public static final MapCodec<PlantHealthBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.fieldOf("health").forGetter(b -> b.health),
			Codec.BOOL.optionalFieldOf("not_pathfindable", false).forGetter(b -> b.notPathfindable)
	).apply(i, PlantHealthBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(BbPlantEvents.ADD, (player, plot, plant) -> {
			plant.state().put(PlantHealth.KEY, new PlantHealth(health));
			if (notPathfindable) {
				plant.state().put(PlantNotPathfindable.KEY, new PlantNotPathfindable());
			}
		});

		events.listen(BbPlantEvents.TICK, (players, plot, plants) -> {
			List<Plant> decayedPlants = new ArrayList<>();
			long ticks = game.ticks();
			boolean update = ticks % 20 == 0;

			for (Plant plant : plants) {
				PlantHealth health = plant.state(PlantHealth.KEY);
				if (health == null) {
					continue;
				}

				if (update) {
					destroyBlockProgress(plot.level, game.random().nextInt(), plant.coverage().getOrigin(), (int) ((1 - health.healthPercent()) * 10.0) - 1);
				}

				if (health.isDead()) {
					for (BlockPos pos : plant.coverage()) {
						BlockState state = plot.level.getBlockState(pos);
						plot.level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));
					}

					decayedPlants.add(plant);
				} else if (update) {
					health.increment(2);
				}
			}

			decayedPlants.forEach(plant -> game.invoker(BbEvents.BREAK_PLANT).breakPlant(players.iterator().next(), plot, plant));
		});
	}

	private static void destroyBlockProgress(ServerLevel level, int id, BlockPos pos, int amt) {
		Vec3 centerPos = Vec3.atCenterOf(pos);
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(centerPos) < 1024.0) {
				player.connection.send(new ClientboundBlockDestructionPacket(id, pos, amt));
			}
		}
	}
}
