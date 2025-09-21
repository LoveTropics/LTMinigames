package com.lovetropics.minigames.common.content.survive_the_tide.behavior;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.content.survive_the_tide.IcebergLine;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressChannel;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressHolder;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressionPeriod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class FormIcebergsGameBehavior implements IGameBehavior {
	public static final MapCodec<FormIcebergsGameBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.fieldOf("height").forGetter(c -> c.height),
			Codec.STRING.optionalFieldOf("lines_region", "iceberg_lines").forGetter(c -> c.linesRegionKey),
			ProgressionPeriod.CODEC.fieldOf("growth_period").forGetter(c -> c.growthPeriod),
			Codec.INT.optionalFieldOf("growth_steps", 0).forGetter(c -> c.maxGrowthSteps)
	).apply(i, FormIcebergsGameBehavior::new));

	private final int height;
	private final String linesRegionKey;
	private final ProgressionPeriod growthPeriod;
	private final int maxGrowthSteps;

	private final List<IcebergLine> icebergLines = new ArrayList<>();
	private int icebergGrowthSteps;

	public FormIcebergsGameBehavior(int height, String linesRegionKey, ProgressionPeriod growthPeriod, int maxGrowthSteps) {
		this.height = height;
		this.linesRegionKey = linesRegionKey;
		this.growthPeriod = growthPeriod;
		this.maxGrowthSteps = maxGrowthSteps;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		for (BlockBox icebergLine : game.mapRegions().get(linesRegionKey)) {
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

		events.listen(GamePhaseEvents.TICK, () -> {
			float progress = progression.progressIn(growthPeriod);
			if (progress <= 0.0f) {
				return;
			}
			int targetSteps = Math.round(progress * maxGrowthSteps);
			if (icebergGrowthSteps < targetSteps) {
				for (IcebergLine line : icebergLines) {
					line.grow(game.level());
				}
				icebergGrowthSteps++;
			}
		});
	}
}
