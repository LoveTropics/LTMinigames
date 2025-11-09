package com.lovetropics.minigames.common.core.game;

import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

public final class GameStopReason {
	private static final GameStopReason FINISHED = new GameStopReason(true, null);
	private static final GameStopReason CANCELED = new GameStopReason(false, null);

	private static final GameStopReason RELOADING = new GameStopReason(false, null);
	private static final GameStopReason SERVER_STOPPING = new GameStopReason(false, null);

	private final boolean finished;
	@Nullable
	private final Component error;

	private GameStopReason(boolean finished, @Nullable Component error) {
		this.finished = finished;
		this.error = error;
	}

	public static GameStopReason finished() {
		return FINISHED;
	}

	public static GameStopReason canceled() {
		return CANCELED;
	}

	public static GameStopReason reloading() {
		return RELOADING;
	}

	public static GameStopReason serverStopping() {
		return SERVER_STOPPING;
	}

	public static GameStopReason errored(Component error) {
		return new GameStopReason(false, error);
	}

	public boolean isFinished() {
		return finished;
	}

	public boolean isCanceled() {
		return !finished;
	}

	public boolean isErrored() {
		return error != null;
	}

	@Nullable
	public Component getError() {
		return error;
	}
}
