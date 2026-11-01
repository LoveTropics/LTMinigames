package org.lovetropics.games.common.core.game.behavior.instances.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameWorldEvents;
import org.lovetropics.games.common.core.game.region.Region;
import org.lovetropics.games.common.core.game.region.RegionQuery;

public record SetupWorldBorderBehavior(
		RegionQuery centerRegion,
		double radius
) implements IGameBehavior {

	public static final MapCodec<SetupWorldBorderBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegionQuery.CODEC.fieldOf("center_region").forGetter(SetupWorldBorderBehavior::centerRegion),
			Codec.DOUBLE.fieldOf("radius").forGetter(SetupWorldBorderBehavior::radius)
	).apply(i, SetupWorldBorderBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		Region centerRegion = this.centerRegion.one(game);
		events.listen(GameWorldEvents.CREATE_LEVEL, level -> {
			level.getWorldBorder().setCenter(centerRegion.centerBlock().getX(), centerRegion.centerBlock().getZ());
			level.getWorldBorder().setSize(radius);
		});
	}
}
