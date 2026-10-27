package org.lovetropics.games.common.content.survive_the_tide.behavior;

import com.lovetropics.lib.BlockBox;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import org.apache.commons.lang3.mutable.MutableInt;
import org.lovetropics.games.common.content.survive_the_tide.IcebergLine;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.region.RegionQuery;
import org.lovetropics.games.common.core.game.state.progress.ProgressChannel;
import org.lovetropics.games.common.core.game.state.progress.ProgressHolder;
import org.lovetropics.games.common.core.game.state.progress.ProgressionPeriod;

import java.util.ArrayList;
import java.util.List;

public record FormIcebergsGameBehavior(
		int height,
		RegionQuery linesRegion,
		ProgressionPeriod growthPeriod,
		int maxGrowthSteps
) implements IGameBehavior {
	public static final MapCodec<FormIcebergsGameBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.fieldOf("height").forGetter(c -> c.height),
			RegionQuery.CODEC.optionalFieldOf("lines_region", RegionQuery.of("iceberg_lines")).forGetter(c -> c.linesRegion),
			ProgressionPeriod.CODEC.fieldOf("growth_period").forGetter(c -> c.growthPeriod),
			Codec.INT.optionalFieldOf("growth_steps", 0).forGetter(c -> c.maxGrowthSteps)
	).apply(i, FormIcebergsGameBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		List<IcebergLine> icebergLines = new ArrayList<>();
		for (BlockBox icebergLine : linesRegion.all(game)) {
			int startX = icebergLine.min().getX();
			int startZ = icebergLine.min().getZ();
			int endX = icebergLine.max().getX();
			int endZ = icebergLine.max().getZ();

			icebergLines.add(new IcebergLine(
					new BlockPos(startX, height, startZ),
					new BlockPos(endX, height, endZ),
					10
			));
			icebergLines.add(new IcebergLine(
					new BlockPos(endX, height, startZ),
					new BlockPos(startX, height, endZ),
					10
			));
		}

		ProgressHolder progression = ProgressChannel.MAIN.getOrThrow(game);

		MutableInt icebergGrowthSteps = new MutableInt();
		events.listen(GamePhaseEvents.TICK, () -> {
			float progress = progression.progressIn(growthPeriod);
			if (progress <= 0.0f) {
				return;
			}
			int targetSteps = Math.round(progress * maxGrowthSteps);
			if (icebergGrowthSteps.intValue() < targetSteps) {
				for (IcebergLine line : icebergLines) {
					line.grow(game.level());
				}
				icebergGrowthSteps.increment();
			}
		});
	}
}
