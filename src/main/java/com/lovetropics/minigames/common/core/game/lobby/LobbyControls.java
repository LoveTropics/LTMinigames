package com.lovetropics.minigames.common.core.game.lobby;

import com.lovetropics.minigames.common.core.game.GameResult;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Unit;

import org.jspecify.annotations.Nullable;
import java.util.EnumMap;
import java.util.Map;

public final class LobbyControls {
	private static final LobbyControls EMPTY = new LobbyControls();

	private final Map<Type, Action> actions = new EnumMap<>(Type.class);

	public static LobbyControls empty() {
		return EMPTY;
	}

	public LobbyControls add(Type type, Action action) {
		actions.put(type, action);
		return this;
	}

	public @Nullable Action get(Type type) {
		return actions.get(type);
	}

	public boolean isEnabled(Type type) {
		return actions.containsKey(type);
	}

	public State asState() {
		State state = State.disabled();
		for (Type type : actions.keySet()) {
			state.enable(type);
		}
		return state;
	}

	public interface Action {
		GameResult<Unit> run();
	}

	public enum Type {
		PLAY,
		SKIP,
		RESTART;

		public int mask() {
			return 1 << ordinal();
		}
	}

	public static final class State {
		private int bits;

		private State(int bits) {
			this.bits = bits;
		}

		public static State disabled() {
			return new State(0);
		}

		public void enable(Type type) {
			bits |= type.mask();
		}

		public void disable(Type type) {
			bits &= ~type.mask();
		}

		public boolean enabled(Type type) {
			return (bits & type.mask()) != 0;
		}

		public void encode(FriendlyByteBuf buffer) {
			buffer.writeByte(bits);
		}

		public static State decode(FriendlyByteBuf buffer) {
			return new State(buffer.readUnsignedByte());
		}
	}
}
