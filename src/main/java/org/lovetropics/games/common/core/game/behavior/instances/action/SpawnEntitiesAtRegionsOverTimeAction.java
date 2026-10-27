package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.level.levelgen.Heightmap;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.region.RegionQuery;
import org.lovetropics.games.common.util.EntityTemplate;

/// Spawns an amount of entities over a set amount of ticks, spread randomly across all the given regions
public class SpawnEntitiesAtRegionsOverTimeAction implements IGameBehavior {
	public static final MapCodec<SpawnEntitiesAtRegionsOverTimeAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegionQuery.CODEC.fieldOf("regions_to_spawn_at").forGetter(c -> c.regionsToSpawnAt),
			EntityTemplate.CODEC.fieldOf("entity").forGetter(c -> c.entity),
			Codec.INT.optionalFieldOf("entity_count", 1).forGetter(c -> c.entityCount),
			Codec.INT.optionalFieldOf("ticks_to_spawn_for", 1).forGetter(c -> c.ticksToSpawnFor),
			Codec.BOOL.optionalFieldOf("at_heightmap", true).forGetter(c -> c.atHeightmap)
	).apply(i, SpawnEntitiesAtRegionsOverTimeAction::new));

	private final RegionQuery regionsToSpawnAt;
	private final EntityTemplate entity;
	private final int entityCount;
	private final int ticksToSpawnFor;
	private final boolean atHeightmap;

	//runtime adjusted vars
	private int ticksRemaining;
	private int entityCountRemaining;

	public SpawnEntitiesAtRegionsOverTimeAction(RegionQuery regionsToSpawnAt, EntityTemplate entity, int entityCount, int ticksToSpawnFor, boolean atHeightmap) {
		this.regionsToSpawnAt = regionsToSpawnAt;
		this.entity = entity;
		this.entityCount = entityCount;
		this.ticksToSpawnFor = ticksToSpawnFor;
		this.atHeightmap = atHeightmap;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
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
				BlockPos pos = regionsToSpawnAt.pickRandomPos(game, game.random());
				if (pos == null) {
					continue;
				}
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
