package org.lovetropics.games.common.core.game.behavior.instances.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.behavior.event.GameWorldEvents;
import org.lovetropics.games.common.core.game.region.Region;
import org.lovetropics.games.common.core.game.region.RegionQuery;
import org.lovetropics.games.common.core.integration.game_actions.GameAction;

import java.util.Optional;

public record ChangeWorldBorderBehavior(
		Optional<RegionQuery> centerRegion,
		Optional<Double> radius
) implements IGameBehavior {

	public static final MapCodec<ChangeWorldBorderBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegionQuery.CODEC.optionalFieldOf("center_region").forGetter(ChangeWorldBorderBehavior::centerRegion),
			Codec.DOUBLE.optionalFieldOf("radius").forGetter(ChangeWorldBorderBehavior::radius)
	).apply(i, ChangeWorldBorderBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			game.allLevels().forEach(level -> {
				centerRegion.ifPresent(region -> {
					Region centerRegion = region.oneIn(game, level);
					level.getWorldBorder().setCenter(centerRegion.centerBlock().getX(), centerRegion.centerBlock().getZ());
				});
				radius.ifPresent(level.getWorldBorder()::setSize);
			});
			return true;
		});
	}
}
