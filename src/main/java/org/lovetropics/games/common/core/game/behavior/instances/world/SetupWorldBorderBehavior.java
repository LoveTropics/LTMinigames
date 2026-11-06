package org.lovetropics.games.common.core.game.behavior.instances.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.border.WorldBorder;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameWorldEvents;
import org.lovetropics.games.common.core.game.region.Region;
import org.lovetropics.games.common.core.game.region.RegionQuery;

import java.util.List;

public record SetupWorldBorderBehavior(
		List<Border> borders
) implements IGameBehavior {

	public static final MapCodec<SetupWorldBorderBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ExtraCodecs.compactListCodec(Border.CODEC).fieldOf("borders").forGetter(SetupWorldBorderBehavior::borders)
	).apply(i, SetupWorldBorderBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameWorldEvents.CREATE_LEVEL, level -> {
			for (Border border : borders) {
				if (border.centerRegion.nullableOneIn(game, level) instanceof Region region) {
					WorldBorder worldBorder = level.getWorldBorder();
					worldBorder.setCenter(region.centerBlock().getX(), region.centerBlock().getZ());
					worldBorder.setSize(border.radius);
					worldBorder.setWarningBlocks(border.warnDistance);
				}
			}
		});
	}

	public record Border(
			RegionQuery centerRegion,
			double radius,
			int warnDistance
	) {
		public static final Codec<Border> CODEC = RecordCodecBuilder.create(i -> i.group(
				RegionQuery.CODEC.fieldOf("center_region").forGetter(Border::centerRegion),
				Codec.DOUBLE.fieldOf("radius").forGetter(Border::radius),
				Codec.INT.optionalFieldOf("warn_distance", 5).forGetter(Border::warnDistance)
		).apply(i, Border::new));

		public static final Border DEFAULT = new Border(RegionQuery.of("center_region"), 1000, 5);
	}
}
