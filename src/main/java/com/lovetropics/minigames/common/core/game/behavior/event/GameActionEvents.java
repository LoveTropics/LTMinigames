package com.lovetropics.minigames.common.core.game.behavior.event;

import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import net.minecraft.util.context.ContextMap;

public final class GameActionEvents {
	public static final GameEventType<Apply> APPLY = GameEventType.create(Apply.class, listeners -> (context, targets) -> {
		boolean applied = false;
		for (Apply listener : listeners) {
			applied |= listener.apply(context, targets);
		}
		return applied;
	});

	private GameActionEvents() {
	}

	public static boolean matches(GameEventType<?> type) {
		return type == APPLY;
	}

	public interface Apply {
		boolean apply(ContextMap context, ActionSubjects<?> targets);
	}
}
