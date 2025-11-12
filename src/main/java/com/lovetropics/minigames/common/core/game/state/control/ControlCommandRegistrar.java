package com.lovetropics.minigames.common.core.game.state.control;

public interface ControlCommandRegistrar {
	default void registerUnrestricted(String name, ControlCommand.Action action) {
		register(name, ControlCommand.Scope.EVERYONE, action);
	}

	default void registerAdmin(String name, ControlCommand.Action action) {
		register(name, ControlCommand.Scope.ADMINS, action);
	}

	void register(String name, ControlCommand.Scope scope, ControlCommand.Action action);
}
