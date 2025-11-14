package com.lovetropics.minigames.common.core.game.behavior.instances.world;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public record IncreaseRandomTickRateInRegionBehaviour(
		List<String> regions,
		int increasedTickRate
) implements IGameBehavior {

	public static final MapCodec<IncreaseRandomTickRateInRegionBehaviour> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.listOf().fieldOf("regions").forGetter(c -> c.regions),
			Codec.INT.fieldOf("increased_tick_rate").forGetter(c -> c.increasedTickRate)
	).apply(i, IncreaseRandomTickRateInRegionBehaviour::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		List<BlockBox> regions = game.mapRegions().getAll(regions());
		events.listen(GamePhaseEvents.TICK, () -> {
			if(game.ticks() % increasedTickRate == 0) {
				for (BlockBox region : regions) {
					for (BlockPos pos : region) {
						BlockState blockState = game.level().getBlockState(pos);
						if (blockState.isRandomlyTicking()) {
							blockState.randomTick(game.level(), pos, game.level().random);
						}
					}
				}
			}
		});
	}
}
