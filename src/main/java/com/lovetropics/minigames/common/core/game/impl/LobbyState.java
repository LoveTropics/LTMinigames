package com.lovetropics.minigames.common.core.game.impl;

import com.lovetropics.minigames.client.lobby.manage.state.ClientCurrentGame;
import com.lovetropics.minigames.client.lobby.state.ClientGameDefinition;
import com.lovetropics.minigames.common.core.game.GameResult;
import com.lovetropics.minigames.common.core.game.GameStopReason;
import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.IGamePhaseDefinition;
import com.lovetropics.minigames.common.core.game.lobby.LobbyControls;
import com.lovetropics.minigames.common.core.game.lobby.QueuedGame;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

abstract class LobbyState {
	protected final GamePhase phase;
	protected final LobbyControls controls = new LobbyControls();

	protected LobbyState(@Nullable GamePhase phase) {
		this.phase = phase;
	}

	protected abstract GameResult<LobbyState> tick(GameLobby lobby);

	@Nullable
	protected ClientCurrentGame getClientCurrentGame() {
		if (phase != null) {
			return new ClientCurrentGame(ClientGameDefinition.from(phase.getDefinition()), null);
		} else {
			return null;
		}
	}

	final Yield yield() {
		return new Yield(phase);
	}

	static class Paused extends LobbyState {
		boolean resume;

		Paused() {
			super(null);
			this.controls.add(LobbyControls.Type.PLAY, () -> {
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
		final ITextComponent error;

		Errored(IGameDefinition game, ITextComponent error) {
			this.game = game;
			this.error = error;
		}

		@Override
		protected ClientCurrentGame getClientCurrentGame() {
			return new ClientCurrentGame(ClientGameDefinition.from(game), error);
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
				pending.pendingGame = game.definition();
				return pending;
			} else {
				return null;
			}
		}

		private CompletableFuture<GameResult<LobbyState>> createGame(GameLobby lobby, IGameDefinition definition) {
			GameInstance game = new GameInstance(lobby, definition);

			IGamePhaseDefinition playing = definition.getPlayingPhase();
			IGamePhaseDefinition waiting = definition.getWaitingPhase();
			if (waiting != null) {
				return this.createWaiting(game, waiting, playing);
			} else {
				return this.createPlaying(game, playing);
			}
		}

		private CompletableFuture<GameResult<LobbyState>> createPlaying(GameInstance game, IGamePhaseDefinition definition) {
			return GamePhase.create(game, definition)
					.thenApply(result -> result.map(Playing::new));
		}

		private CompletableFuture<GameResult<LobbyState>> createWaiting(GameInstance game, IGamePhaseDefinition definition, IGamePhaseDefinition playing) {
			return GamePhase.create(game, definition)
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
			this.controls.add(LobbyControls.Type.SKIP, () -> phase.requestStop(GameStopReason.canceled()));
		}

		@Nullable
		@Override
		protected GameResult<LobbyState> tick(GameLobby lobby) {
			GameStopReason stopping = phase.tick();
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

			this.controls.add(LobbyControls.Type.PLAY, () -> phase.requestStop(GameStopReason.finished()));
			this.controls.add(LobbyControls.Type.SKIP, () -> phase.requestStop(GameStopReason.canceled()));
		}

		@Override
		protected GameResult<LobbyState> tick(GameLobby lobby) {
			GameStopReason stopping = phase.tick();
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
		IGameDefinition pendingGame;

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
			if (pendingGame != null) {
				return new ClientCurrentGame(ClientGameDefinition.from(pendingGame), null);
			} else {
				return super.getClientCurrentGame();
			}
		}
	}
}
