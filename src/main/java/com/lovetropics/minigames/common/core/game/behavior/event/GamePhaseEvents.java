package com.lovetropics.minigames.common.core.game.behavior.event;

import com.lovetropics.minigames.common.core.game.GameStopReason;
import com.lovetropics.minigames.common.core.game.state.control.ControlCommandRegistrar;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;

import javax.annotation.Nullable;

public final class GamePhaseEvents {
	public static final GameEventType<Create> CREATE = GameEventType.create(Create.class, listeners -> () -> {
		for (Create listener : listeners) {
			listener.create();
		}
	});

	public static final GameEventType<Destroy> DESTROY = GameEventType.create(Destroy.class, listeners -> () -> {
		for (Destroy listener : listeners) {
			listener.destroy();
		}
	});

	public static final GameEventType<Start> START = GameEventType.create(Start.class, listeners -> initiator -> {
		for (Start listener : listeners) {
			listener.start(initiator);
		}
	});

	public static final GameEventType<Stop> STOP = GameEventType.create(Stop.class, listeners -> reason -> {
		for (Stop listener : listeners) {
			listener.stop(reason);
		}
	});

	public static final GameEventType<Finish> FINISH = GameEventType.create(Finish.class, listeners -> () -> {
		for (Finish listener : listeners) {
			listener.finish();
		}
	});

	public static final GameEventType<Tick> TICK = GameEventType.create(Tick.class, listeners -> () -> {
		for (Tick listener : listeners) {
			listener.tick();
		}
	});

	public static final GameEventType<RegisterCommands> REGISTER_COMMANDS = GameEventType.create(RegisterCommands.class, listeners -> commands -> {
		for (RegisterCommands listener : listeners) {
			listener.register(commands);
		}
	});

	private GamePhaseEvents() {
	}

	public interface Create {
		void create();
	}

	public interface Start {
		void start(@Nullable PlayerKey initiator);
	}

	public interface Stop {
		void stop(GameStopReason reason);
	}

	public interface Destroy {
		void destroy();
	}

	public interface Finish {
		void finish();
	}

	public interface Tick {
		void tick();
	}

	public interface RegisterCommands {
		void register(ControlCommandRegistrar commands);
	}
}
