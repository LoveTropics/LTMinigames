package org.lovetropics.games.common.core.game.state;

public final class TimedGameState implements IGameState {
	public static final GameStateKey<TimedGameState> KEY = GameStateKey.create("Timed Game");

	private final long closeDuration;

	private long ticksRemaining;
	private long closeAtTime;
	private boolean paused;

	public TimedGameState(long length, long closeDuration) {
		ticksRemaining = length;
		this.closeDuration = closeDuration;
	}

	public void setPaused(boolean paused) {
		this.paused = paused;
	}

	public void setTicksRemaining(long ticksRemaining) {
		this.ticksRemaining = ticksRemaining;
	}

	public long getTicksRemaining() {
		return ticksRemaining;
	}

	public void increaseRemaining(long add) {
		ticksRemaining = Math.max(ticksRemaining + add, 1);
	}

	public boolean isPaused() {
		return paused;
	}

	public TickResult tick(long time) {
		if (closeAtTime != 0 && time >= closeAtTime) {
			return TickResult.CLOSE;
		}

		if (!paused && ticksRemaining > 0) {
			return tickRunning(time);
		} else {
			return TickResult.PAUSED;
		}
	}

	private TickResult tickRunning(long time) {
		long ticksRemaining = --this.ticksRemaining;
		if (ticksRemaining == 0) {
			closeAtTime = time + closeDuration;
			return TickResult.GAME_OVER;
		}

		return TickResult.RUNNING;
	}

	public enum TickResult {
		RUNNING,
		PAUSED,
		GAME_OVER,
		CLOSE,
	}
}
