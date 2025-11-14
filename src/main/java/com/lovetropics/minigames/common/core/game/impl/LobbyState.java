package com.lovetropics.minigames.common.core.game.impl;

import com.lovetropics.lib.slideshow.SlideshowApi;
import com.lovetropics.lib.slideshow.SlideshowInstanceHandle;
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
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

abstract class LobbyState {
	@Nullable
	protected final GamePhase phase;
	protected final LobbyControls controls = new LobbyControls();

	protected LobbyState(@Nullable GamePhase phase) {
		this.phase = phase;
	}

	@Nullable
	protected GamePhaseType phaseType() {
		return null;
	}

	protected abstract GameResult<LobbyState> tick(GameLobby lobby);

	@Nullable
	protected ClientCurrentGame getClientCurrentGame() {
		if (phase != null) {
			GamePhaseType phaseType = Objects.requireNonNullElse(phaseType(), GamePhaseType.WAITING);
			return ClientCurrentGame.create(phase, phaseType);
		}
		return null;
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
				Pending pending = createGame(lobby, phase, game.definition());
				pending.pendingGame = ClientCurrentGame.create(
						ClientGameDefinition.from(game.definition()),
						GamePhaseType.WAITING
				);
				return pending;
			} else {
				return null;
			}
		}

		private Pending createGame(GameLobby lobby, @Nullable GamePhase lastPhase, IGameDefinition definition) {
			GameInstance game = new GameInstance(lobby, definition);
			game.stateMap.register(GameRewardsMap.STATE, lobby.getRewardsMap());

			final IGamePhaseDefinition playingDefinition = definition.getPlayingPhase();
			Optional<IGamePhaseDefinition> waitingDefinition = definition.getWaitingPhase();
			if (waitingDefinition.isPresent()) {
				CompletableFuture<LobbyState> waiting = createWaiting(lobby, game, waitingDefinition.get(), playingDefinition);
				return new Pending(lastPhase, waiting, null);
			} else {
				CompletableFuture<LobbyState> playing = createPlaying(game, playingDefinition);
				return new Pending(lastPhase, playing, openIntroSlideshow(lobby, game));
			}
		}

		private CompletableFuture<LobbyState> createPlaying(GameInstance game, IGamePhaseDefinition definition) {
			return GamePhaseManager.get().createPhase(game, game.server(), game.definition, definition).thenApply(Playing::new);
		}

		private CompletableFuture<LobbyState> createWaiting(GameLobby lobby, GameInstance game, IGamePhaseDefinition definition, IGamePhaseDefinition playing) {
			return GamePhaseManager.get().createPhase(game, game.server(), game.definition, definition)
					.thenApply(waiting -> {
						Supplier<LobbyState> start = () -> {
							CompletableFuture<LobbyState> next = createPlaying(waiting.game, playing);
							return new LobbyState.Pending(waiting, next, openIntroSlideshow(lobby, game));
						};
						return new LobbyState.Waiting(waiting, start);
					});
		}

		private @Nullable SlideshowInstanceHandle openIntroSlideshow(GameLobby lobby, GameInstance game) {
			ResourceLocation slideshowId = game.definition.introSlideshow();
			SlideshowInstanceHandle slideshow = slideshowId != null ? SlideshowApi.open(slideshowId) : null;
			if (slideshow != null) {
				slideshow.play();
				lobby.getPlayers().forEach(slideshow::addPlayer);
			}
			return slideshow;
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
			GameStopReason stopReason = Objects.requireNonNull(phase).stopReason();
			if (stopReason != null) {
				return nextState(stopReason);
			}
			return GameResult.ok(this);
		}

		private GameResult<LobbyState> nextState(GameStopReason stopping) {
			if (!stopping.isErrored()) {
				return GameResult.ok(this.yield());
			} else {
				return GameResult.error(stopping.getError());
			}
		}

		@Override
		protected GamePhaseType phaseType() {
			return GamePhaseType.PLAYING;
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
			GameStopReason stopReason = Objects.requireNonNull(phase).stopReason();
			if (stopReason != null) {
				return nextState(stopReason);
			}
			return GameResult.ok(this);
		}

		private GameResult<LobbyState> nextState(GameStopReason stopping) {
			if (!stopping.isErrored()) {
				return GameResult.ok(stopping.isFinished() ? start.get() : this.yield());
			} else {
				return GameResult.error(stopping.getError());
			}
		}

		@Override
		protected GamePhaseType phaseType() {
			return GamePhaseType.WAITING;
		}
	}

	static final class Pending extends LobbyState {
		private static final double SLIDESHOW_BUFFER_TIME = 0.5;

		final CompletableFuture<GameResult<LobbyState>> next;
		@Nullable
		final SlideshowInstanceHandle slideshow;
		@Nullable
		ClientCurrentGame pendingGame;

		Pending(@Nullable GamePhase phase, CompletableFuture<LobbyState> next, @Nullable SlideshowInstanceHandle slideshow) {
			super(phase);
			this.next = GameResult.handleException(next);
			this.slideshow = slideshow;
		}

		@Override
		protected GameResult<LobbyState> tick(GameLobby lobby) {
			if (slideshow != null && slideshow.currentTime() < slideshow.totalTime() - SLIDESHOW_BUFFER_TIME) {
				return GameResult.ok(this);
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
