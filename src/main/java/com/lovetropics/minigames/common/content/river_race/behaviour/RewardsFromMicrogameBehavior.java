package com.lovetropics.minigames.common.content.river_race.behaviour;

import com.lovetropics.minigames.common.content.river_race.microgames.MicrogameEvents;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.rewards.GameRewardsMap;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerPlayer;

public record RewardsFromMicrogameBehavior() implements IGameBehavior {
	public static final MapCodec<RewardsFromMicrogameBehavior> CODEC = MapCodec.unit(RewardsFromMicrogameBehavior::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(MicrogameEvents.MICROGAMES_ENDED, () -> {
			GameRewardsMap rewards = game.instanceState().getOrThrow(GameRewardsMap.STATE);
			for (ServerPlayer participant : game.participants()) {
				rewards.grant(participant);
			}
			rewards.clear();
		});
	}
}
