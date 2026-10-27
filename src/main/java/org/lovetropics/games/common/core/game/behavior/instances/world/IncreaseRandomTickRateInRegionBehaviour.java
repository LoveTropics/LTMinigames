package org.lovetropics.games.common.core.game.behavior.instances.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.region.RegionQuery;

public record IncreaseRandomTickRateInRegionBehaviour(
		RegionQuery regions,
		int increasedTickRate
) implements IGameBehavior {
	public static final MapCodec<IncreaseRandomTickRateInRegionBehaviour> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegionQuery.CODEC.fieldOf("regions").forGetter(c -> c.regions),
			Codec.INT.fieldOf("increased_tick_rate").forGetter(c -> c.increasedTickRate)
	).apply(i, IncreaseRandomTickRateInRegionBehaviour::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		ServerLevel level = game.level();
		events.listen(GamePhaseEvents.TICK, ()-> {
			if (game.ticks() % increasedTickRate == 0) {
				regions.forEachBlock(game, pos -> {
					BlockState blockState = level.getBlockState(pos);
					if (blockState.isRandomlyTicking()) {
						blockState.randomTick(level, pos, game.random());
					}
				});
			}
		});
	}
}
