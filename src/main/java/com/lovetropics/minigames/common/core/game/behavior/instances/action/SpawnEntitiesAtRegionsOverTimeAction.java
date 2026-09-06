package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.google.common.collect.Lists;
import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.map.MapRegions;
import com.lovetropics.minigames.common.util.EntityTemplate;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

/// Spawns an amount of entities over a set amount of ticks, spread randomly across all the given regions
public class SpawnEntitiesAtRegionsOverTimeAction implements IGameBehavior {
	public static final MapCodec<SpawnEntitiesAtRegionsOverTimeAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.listOf().fieldOf("regions_to_spawn_at").forGetter(c -> c.regionsToSpawnAtKeys),
			EntityTemplate.CODEC.fieldOf("entity").forGetter(c -> c.entity),
			Codec.INT.optionalFieldOf("entity_count", 1).forGetter(c -> c.entityCount),
			Codec.INT.optionalFieldOf("ticks_to_spawn_for", 1).forGetter(c -> c.ticksToSpawnFor),
			Codec.BOOL.optionalFieldOf("at_heightmap", true).forGetter(c -> c.atHeightmap)
	).apply(i, SpawnEntitiesAtRegionsOverTimeAction::new));

	private final List<String> regionsToSpawnAtKeys;
	private final EntityTemplate entity;
	private final int entityCount;
	private final int ticksToSpawnFor;
	private final boolean atHeightmap;

	//runtime adjusted vars
	private int ticksRemaining;
	private int entityCountRemaining;

	private final List<BlockBox> regionsToSpawnAt = Lists.newArrayList();

	public SpawnEntitiesAtRegionsOverTimeAction(final List<String> regionsToSpawnAtKeys, final EntityTemplate entity, final int entityCount, final int ticksToSpawnFor, final boolean atHeightmap) {
		this.regionsToSpawnAtKeys = regionsToSpawnAtKeys;
		this.entity = entity;
		this.entityCount = entityCount;
		this.ticksToSpawnFor = ticksToSpawnFor;
		this.atHeightmap = atHeightmap;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		MapRegions regions = game.mapRegions();

		regionsToSpawnAt.clear();
		for (String key : regionsToSpawnAtKeys) {
			regionsToSpawnAt.addAll(regions.get(key));
		}

		events.listen(GameActionEvents.APPLY, (context, targets) -> applyPackage(context));
		events.listen(GamePhaseEvents.TICK, () -> tick(game));
	}

	private boolean applyPackage(ContextMap context) {
		ticksRemaining += ticksToSpawnFor;
		entityCountRemaining += entityCount;
		return true;
	}

	private void tick(IGamePhase game) {
		if (ticksRemaining > 0) {

			//TODO: support less than 1 spawned per tick rate
			//might have a few left unspawned if the rate is real high and not perfectly divisible, but fine for lightning storm
			int spawnsPerTick = Math.max(1, entityCountRemaining / ticksRemaining);

			//System.out.println("spawnsPerTick: " + spawnsPerTick + ", ticksRemaining: " + ticksRemaining);

			ServerLevel level = game.level();
			for (int i = 0; i < spawnsPerTick; i++) {
				BlockBox region = regionsToSpawnAt.get(level.getRandom().nextInt(regionsToSpawnAt.size()));
				BlockPos pos = region.sample(level.getRandom());
				if (atHeightmap) {
					pos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, pos);
				}

				entity.spawn(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
				entityCountRemaining--;
			}

			ticksRemaining--;
		}
	}
}
