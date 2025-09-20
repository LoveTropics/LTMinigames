package com.lovetropics.minigames.common.core.game.behavior.instances.statistics;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.state.statistics.GameStatistics;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.Entity;

public final class DamageTrackerBehavior implements IGameBehavior {
	public static final MapCodec<DamageTrackerBehavior> CODEC = MapCodec.unit(DamageTrackerBehavior::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GamePlayerEvents.DAMAGE, (player, source, damageAmount) -> {
			GameStatistics statistics = game.statistics();

			statistics.forPlayer(player)
					.withDefault(StatisticKey.DAMAGE_TAKEN, () -> 0.0F)
					.apply(total -> total + damageAmount);

			Entity attacker = source.getEntity();
			if (attacker instanceof ServerPlayer attackerPlayer) {
				statistics.forPlayer(attackerPlayer)
						.withDefault(StatisticKey.DAMAGE_DEALT, () -> 0.0F)
						.apply(total -> total + damageAmount);
			}

			return TriState.DEFAULT;
		});
	}
}
