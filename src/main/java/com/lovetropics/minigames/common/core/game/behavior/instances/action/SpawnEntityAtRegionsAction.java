package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.map.MapRegions;
import com.lovetropics.minigames.common.util.EntityTemplate;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

public class SpawnEntityAtRegionsAction implements IGameBehavior {
	public static final MapCodec<SpawnEntityAtRegionsAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.listOf().fieldOf("regions_to_spawn_at").forGetter(c -> c.regionsToSpawnAtKeys),
			EntityTemplate.CODEC.fieldOf("entity").forGetter(c -> c.entity),
			Codec.INT.optionalFieldOf("entity_count_per_region", 1).forGetter(c -> c.entityCountPerRegion),
			Codec.BOOL.optionalFieldOf("at_heightmap", true).forGetter(c -> c.atHeightmap)
	).apply(i, SpawnEntityAtRegionsAction::new));

	private final List<String> regionsToSpawnAtKeys;
	private final EntityTemplate entity;
	private final int entityCountPerRegion;
	private final boolean atHeightmap;

	public SpawnEntityAtRegionsAction(final List<String> regionsToSpawnAtKeys, final EntityTemplate entity, final int entityCountPerRegion, boolean atHeightmap) {
		this.regionsToSpawnAtKeys = regionsToSpawnAtKeys;
		this.entity = entity;
		this.entityCountPerRegion = entityCountPerRegion;
		this.atHeightmap = atHeightmap;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		MapRegions regions = game.mapRegions();

		List<BlockBox> regionsToSpawnAt = new ArrayList<>();
		for (String key : regionsToSpawnAtKeys) {
			regionsToSpawnAt.addAll(regions.get(key));
		}

		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			if (regionsToSpawnAt.isEmpty()) {
				return false;
			}

			ServerLevel world = game.level();
			for (final BlockBox region : regionsToSpawnAt) {
				for (int i = 0; i < entityCountPerRegion; i++) {
					BlockPos pos = region.sample(world.getRandom());
					if (atHeightmap) {
						pos = world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, pos);
					}
					entity.spawn(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
				}
			}

			return true;
		});
	}
}
