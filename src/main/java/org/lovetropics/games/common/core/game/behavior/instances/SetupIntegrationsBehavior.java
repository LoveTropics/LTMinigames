package org.lovetropics.games.common.core.game.behavior.instances;

import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.GameStopReason;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameLogicEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.state.GameStateMap;
import org.lovetropics.games.common.core.game.util.GameTexts;
import org.lovetropics.games.common.core.integration.BackendIntegrations;
import org.lovetropics.games.common.core.integration.GameInstanceIntegrations;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public record SetupIntegrationsBehavior(
		Optional<Identifier> backendId,
		Optional<String> statisticsKey
) implements IGameBehavior {
	public static final MapCodec<SetupIntegrationsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Identifier.CODEC.optionalFieldOf("backend_id").forGetter(SetupIntegrationsBehavior::backendId),
			Codec.STRING.optionalFieldOf("statistics_key").forGetter(SetupIntegrationsBehavior::statisticsKey)
	).apply(i, SetupIntegrationsBehavior::new));

	// TODO: we could potentially have state entries & the IGamePhase come through the constructor with codec hacks
	@Override
	public void registerState(IGamePhase game, GameStateMap phaseState, GameStateMap instanceState) {
		if (game.isFocusedLive()) {
			if (!BackendIntegrations.get().isConnected()) {
				throw new GameException(GameTexts.Status.integrationsNotConnected());
			}
			Identifier backendId = this.backendId.orElse(game.definition().id());
			String statisticsKey = this.statisticsKey.orElse(game.definition().id().getPath());
			BackendIntegrations.get().open(instanceState, game, backendId, statisticsKey);
		}
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		GameInstanceIntegrations integrations = game.instanceState().getOrNull(GameInstanceIntegrations.KEY);
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
