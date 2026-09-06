package com.lovetropics.minigames.common.core.game;

import net.minecraft.network.chat.Component;

import org.jspecify.annotations.Nullable;
import java.util.concurrent.CompletionException;

public class GameException extends RuntimeException {
	private final Component message;

	public GameException(Component message) {
		super(message.getString());
		this.message = message;
	}

	public GameException(Component message, Throwable cause) {
		super(message.getString(), cause);
		this.message = message;
	}

	public Component getTextMessage() {
		return message;
	}

	public static @Nullable GameException unwrap(@Nullable Throwable throwable) {
		if (throwable instanceof GameException exception) {
			return exception;
		} else if (throwable instanceof CompletionException exception) {
			return unwrap(exception.getCause());
		}
		return null;
	}
}
