package com.lovetropics.minigames.common.core.game.impl;

import com.lovetropics.minigames.client.lobby.state.ClientCurrentGame;
import com.lovetropics.minigames.client.lobby.state.ClientGameDefinition;
import com.lovetropics.minigames.common.core.game.GamePhaseType;
import com.lovetropics.minigames.common.core.game.GameResult;
import com.lovetropics.minigames.common.core.game.GameStopReason;
import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.IGamePhaseDefinition;
import com.lovetropics.minigames.common.core.game.lobby.LobbyControls;
import com.lovetropics.minigames.common.core.game.lobby.QueuedGame;
import com.lovetropics.minigames.common.core.game.rewards.GameRewardsMap;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

abstract class LobbyState {
	@Nullable
	protected final GamePhase phase;
	protected final LobbyControls controls = new LobbyControls();

	protected LobbyState(@Nullable GamePhase phase) {
		this.phase = phase;
	}

	protected abstract GameResult<LobbyState> tick(GameLobby lobby);

	@Nullable
	protected ClientCurrentGame getClientCurrentGame() {
		return phase != null ? ClientCurrentGame.create(phase) : null;
	}

	final Yield yield() {
		return new Yield(phase);
	}

	static class Paused extends LobbyState {
		boolean resume;

		Paused() {
			super(null);
			controls.add(LobbyControls.Type.PLAY, () -> {
				resume = true;
				return GameResult.ok();
			});
		}

		@Override
		protected GameResult<LobbyState> tick(GameLobby lobby) {
			return GameResult.ok(resume ? this.yield() : this);
		}
	}

	static final class Errored extends Paused {
		final IGameDefinition game;
		final GamePhaseType phaseType;
		final Component error;

		Errored(IGameDefinition game, GamePhaseType phaseType, Component error) {
			this.game = game;
			this.phaseType = phaseType;
			this.error = error;
		}

		@Override
		protected ClientCurrentGame getClientCurrentGame() {
			ClientGameDefinition definition = ClientGameDefinition.from(game);
			return ClientCurrentGame.create(definition, phaseType).withError(error);
		}
	}

	static final class Closed extends LobbyState {
		Closed() {
			super(null);
		}

		@Override
		protected GameResult<LobbyState> tick(GameLobby lobby) {
			return GameResult.ok(this);
		}
	}

	static final class Yield extends LobbyState {
		Yield(@Nullable GamePhase phase) {
			super(phase);
		}

		@Override
		protected GameResult<LobbyState> tick(GameLobby lobby) {
			LobbyState pending = nextGameState(lobby, phase);
			return pending != null ? GameResult.ok(pending) : GameResult.ok(new Paused());
		}

		@Nullable
		private LobbyState nextGameState(GameLobby lobby, @Nullable GamePhase phase) {
			QueuedGame game = lobby.gameQueue.next();
			if (game != null) {
				Pending pending = new Pending(phase, createGame(lobby, game.definition()));
				pending.pendingGame = ClientCurrentGame.create(
						ClientGameDefinition.from(game.definition()),
						GamePhaseType.WAITING
				);
				return pending;
			} else {
				return null;
			}
		}

		private CompletableFuture<GameResult<LobbyState>> createGame(GameLobby lobby, IGameDefinition definition) {
			GameInstance game = new GameInstance(lobby, definition);
			game.stateMap.register(GameRewardsMap.STATE, lobby.getRewardsMap());

			final IGamePhaseDefinition playing = definition.getPlayingPhase();
			return definition.getWaitingPhase()
					.map(ph -> createWaiting(game, ph, playing))
					.orElseGet(() -> createPlaying(game, playing));
		}

		private CompletableFuture<GameResult<LobbyState>> createPlaying(GameInstance game, IGamePhaseDefinition definition) {
			return GamePhase.create(game, game.definition, definition, GamePhaseType.PLAYING)
					.thenApply(result -> result.map(Playing::new));
		}

		private CompletableFuture<GameResult<LobbyState>> createWaiting(GameInstance game, IGamePhaseDefinition definition, IGamePhaseDefinition playing) {
			return GamePhase.create(game, game.definition, definition, GamePhaseType.WAITING)
					.thenApply(result -> result.map(waiting -> {
						Supplier<LobbyState> start = () -> {
							CompletableFuture<GameResult<LobbyState>> next = createPlaying(waiting.game, playing);
							return new LobbyState.Pending(waiting, next);
						};
						return new LobbyState.Waiting(waiting, start);
					}));
		}
	}

	static final class Playing extends LobbyState {
		Playing(GamePhase phase) {
			super(phase);
			controls.add(LobbyControls.Type.SKIP, () -> phase.requestStop(GameStopReason.canceled()));
			controls.add(LobbyControls.Type.RESTART, () -> phase.requestStop(GameStopReason.canceled()));
		}

		@Override
		protected GameResult<LobbyState> tick(GameLobby lobby) {
			GameStopReason stopping = Objects.requireNonNull(phase).tick();
			if (stopping == null) {
				return GameResult.ok(this);
			} else {
				return nextState(stopping);
			}
		}

		private GameResult<LobbyState> nextState(GameStopReason stopping) {
			if (!stopping.isErrored()) {
				return GameResult.ok(this.yield());
			} else {
				return GameResult.error(stopping.getError());
			}
		}
	}

	static final class Waiting extends LobbyState {
		final Supplier<LobbyState> start;

		Waiting(GamePhase phase, Supplier<LobbyState> start) {
			super(phase);
			this.start = start;

			controls.add(LobbyControls.Type.PLAY, () -> phase.requestStop(GameStopReason.finished()));
			controls.add(LobbyControls.Type.SKIP, () -> phase.requestStop(GameStopReason.canceled()));
			// TODO stuffs
			controls.add(LobbyControls.Type.RESTART, () -> {
				// TODO queue another game up
				return phase.requestStop(GameStopReason.canceled());
			});
		}

		@Override
		protected GameResult<LobbyState> tick(GameLobby lobby) {
			GameStopReason stopping = Objects.requireNonNull(phase).tick();
			if (stopping == null) {
				return GameResult.ok(this);
			} else {
				return nextState(stopping);
			}
		}

		private GameResult<LobbyState> nextState(GameStopReason stopping) {
			if (!stopping.isErrored()) {
				return GameResult.ok(stopping.isFinished() ? start.get() : this.yield());
			} else {
				return GameResult.error(stopping.getError());
			}
		}
	}

	static final class Pending extends LobbyState {
		final CompletableFuture<GameResult<LobbyState>> next;
		@Nullable
		ClientCurrentGame pendingGame;

		Pending(@Nullable GamePhase phase, CompletableFuture<GameResult<LobbyState>> next) {
			super(phase);
			this.next = next;
		}

		@Override
		protected GameResult<LobbyState> tick(GameLobby lobby) {
			if (phase != null) {
				phase.tick();
			}
			return next.getNow(GameResult.ok(this));
		}

		@Nullable
		@Override
		protected ClientCurrentGame getClientCurrentGame() {
			return pendingGame != null ? pendingGame : super.getClientCurrentGame();
		}
	}
}
