package org.lovetropics.games.common.core.game.behavior.instances.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.border.WorldBorder;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.behavior.event.GameWorldEvents;
import org.lovetropics.games.common.core.game.region.Region;
import org.lovetropics.games.common.core.game.region.RegionQuery;
import org.lovetropics.games.common.core.integration.game_actions.GameAction;

import javax.swing.text.html.Option;
import java.util.Optional;

public record ChangeWorldBorderBehavior(
		RegionQuery centerRegion,
		Optional<Double> radius,
		Optional<Long> ticks
) implements IGameBehavior {

	public static final MapCodec<ChangeWorldBorderBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegionQuery.CODEC.fieldOf("center_region").forGetter(ChangeWorldBorderBehavior::centerRegion),
			Codec.DOUBLE.optionalFieldOf("radius").forGetter(ChangeWorldBorderBehavior::radius),
			Codec.LONG.optionalFieldOf("ticks").forGetter(ChangeWorldBorderBehavior::ticks)
	).apply(i, ChangeWorldBorderBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			game.allLevels().forEach(level -> {
				// Iterate for every level, if we find a center region, set the center.
				if (centerRegion.nullableOneIn(game, level) instanceof Region region) {
					WorldBorder worldBorder = level.getWorldBorder();
					worldBorder.setCenter(region.centerBlock().getX(), region.centerBlock().getZ());
					if (radius.isPresent()) {
						if (ticks.isPresent()) {
							worldBorder.lerpSizeBetween(worldBorder.getSize(), radius.get(), ticks.get(), level.getGameTime());
						} else {
							worldBorder.setSize(radius.get());
						}
					} else {
						worldBorder.setSize(WorldBorder.Settings.DEFAULT.size());
					}
				}
			});
			return true;
		});
	}
}
