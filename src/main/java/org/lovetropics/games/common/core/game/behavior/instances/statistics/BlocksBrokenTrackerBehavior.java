package org.lovetropics.games.common.core.game.behavior.instances.statistics;

import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.TriState;

public final class BlocksBrokenTrackerBehavior implements IGameBehavior {
	public static final MapCodec<BlocksBrokenTrackerBehavior> CODEC = MapCodec.unit(BlocksBrokenTrackerBehavior::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GamePlayerEvents.BREAK_BLOCK, (player, pos, state, hand) -> {
			game.statistics().forPlayer(player)
					.incrementInt(StatisticKey.BLOCKS_BROKEN, 1);
			return TriState.DEFAULT;
		});
	}
}
