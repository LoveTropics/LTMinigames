package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.GameStopReason;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLogicEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.state.GameStateMap;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import com.lovetropics.minigames.common.core.integration.BackendIntegrations;
import com.lovetropics.minigames.common.core.integration.GameInstanceIntegrations;
import com.mojang.serialization.MapCodec;

import org.jspecify.annotations.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SetupIntegrationsBehavior implements IGameBehavior {
	public static final MapCodec<SetupIntegrationsBehavior> CODEC = MapCodec.unit(SetupIntegrationsBehavior::new);

	private @Nullable GameInstanceIntegrations integrations;

	// TODO: we could potentially have state entries & the IGamePhase come through the constructor with codec hacks
	@Override
	public void registerState(IGamePhase game, GameStateMap phaseState, GameStateMap instanceState) {
		if (game.isFocusedLive()) {
			if (!BackendIntegrations.get().isConnected()) {
				throw new GameException(GameTexts.Status.integrationsNotConnected());
			}
			integrations = BackendIntegrations.get().getOrOpen(instanceState, game);
		}
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		if (integrations == null) {
			return;
		}

		events.listen(GamePhaseEvents.START, initiator ->
				integrations.start(game, events, initiator)
		);

		AtomicBoolean finished = new AtomicBoolean();
		events.listen(GameLogicEvents.GAME_OVER, winner -> {
			// TODO Hackfix: run at the end of the tick so that all behaviors can respond to the game over event first
			game.scheduler().runAfterTicks(0, () -> {
				if (finished.compareAndSet(false, true)) {
					integrations.finish(game);
				}
			});
		});

		events.listen(GamePhaseEvents.STOP, reason -> {
			if (!finished.compareAndSet(false, true)) {
				return;
			}
			if (reason == GameStopReason.finished()) {
				integrations.finish(game);
			} else {
				integrations.cancel(game);
			}
		});
	}
}
