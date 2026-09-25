package org.lovetropics.games.lobbies;

import com.lovetropics.lib.slideshow.SlideshowApi;
import com.lovetropics.lib.slideshow.SlideshowInstanceHandle;
import org.lovetropics.games.common.core.game.GamePhaseType;
import org.lovetropics.games.common.core.game.GameResult;
import org.lovetropics.games.common.core.game.GameStopReason;
import org.lovetropics.games.common.core.game.config.GameConfig;
import org.lovetropics.games.common.core.game.config.GamePhaseConfig;
import org.lovetropics.games.common.core.game.rewards.GameRewardsMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.lobbies.client.ClientGameDefinition;
import org.lovetropics.games.lobbies.client.state.ClientCurrentGame;
import org.lovetropics.games.lobbies.dev.DevQuickPlay;
import org.lovetropics.games.lobbies.dev.DevQuickPlaySettings;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

abstract class LobbyState {
	protected final @Nullable GamePhase phase;
	protected final LobbyControls controls = new LobbyControls();

	protected LobbyState(@Nullable GamePhase phase) {
		this.phase = phase;
	}

	protected @Nullable GamePhaseType phaseType() {
		return null;
	}

	protected abstract GameResult<LobbyState> tick(GameLobby lobby);

	protected @Nullable ClientCurrentGame getClientCurrentGame() {
		if (phase != null) {
			GamePhaseType phaseType = Objects.requireNonNullElse(phaseType(), GamePhaseType.WAITING);
			return new ClientCurrentGame(ClientGameDefinition.from(phase.game.config()), phaseType);
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
		final GameConfig game;
		final Component error;

		Errored(GameConfig game, Component error) {
			this.game = game;
			this.error = error;
		}

		@Override
		protected ClientCurrentGame getClientCurrentGame() {
			ClientGameDefinition definition = ClientGameDefinition.from(game);
			return new ClientCurrentGame(definition, GamePhaseType.WAITING).withError(error);
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

			private @Nullable LobbyState nextGameState(GameLobby lobby, @Nullable GamePhase phase) {
			QueuedGame game = lobby.gameQueue.next();
			if (game != null) {
				Pending pending = createGame(lobby, phase, game.config());
				pending.pendingGame = new ClientCurrentGame(
						ClientGameDefinition.from(game.config()),
						GamePhaseType.WAITING
				);
				return pending;
			} else {
				return null;
			}
		}

		private Pending createGame(GameLobby lobby, @Nullable GamePhase lastPhase, GameConfig config) {
			GameInstance game = new GameInstance(lobby, config);
			game.instanceState().register(GameRewardsMap.STATE, lobby.getRewardsMap());

			GamePhaseConfig playingDefinition = config.playing();
			if (config.waiting() != null) {
				CompletableFuture<LobbyState> waiting = createWaiting(lobby, game, config.waiting(), playingDefinition);
				return new Pending(lastPhase, waiting, null);
			} else {
				CompletableFuture<LobbyState> playing = createPlaying(game, playingDefinition);
				return new Pending(lastPhase, playing, openIntroSlideshow(lobby, game));
			}
		}

		private CompletableFuture<LobbyState> createPlaying(GameInstance game, GamePhaseConfig config) {
			return GamePhaseManager.get().createTopPhase(game, config).thenApply(Playing::new);
		}

		private CompletableFuture<LobbyState> createWaiting(GameLobby lobby, GameInstance game, GamePhaseConfig config, GamePhaseConfig playing) {
			return GamePhaseManager.get().createTopPhase(game, config)
					.thenApply(waiting -> {
						Supplier<LobbyState> start = () -> {
							CompletableFuture<LobbyState> next = createPlaying(game, playing);
							return new LobbyState.Pending(waiting, next, openIntroSlideshow(lobby, game));
						};
						return new LobbyState.Waiting(waiting, start);
					});
		}

		private @Nullable SlideshowInstanceHandle openIntroSlideshow(GameLobby lobby, GameInstance game) {
			if (DevQuickPlay.isEnabled() && DevQuickPlaySettings.SKIP_INTRO_SLIDESHOWS) {
				return null;
			}
			Identifier slideshowId = game.config().introSlideshow();
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
			GamePhase phase = Objects.requireNonNull(this.phase);
			if (DevQuickPlay.isEnabled()) {
				phase.requestStop(GameStopReason.finished());
			}
			GameStopReason stopReason = phase.stopReason();
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
		final @Nullable SlideshowInstanceHandle slideshow;
		@Nullable ClientCurrentGame pendingGame;

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

		@Override
		protected @Nullable ClientCurrentGame getClientCurrentGame() {
			return pendingGame != null ? pendingGame : super.getClientCurrentGame();
		}
	}
}
