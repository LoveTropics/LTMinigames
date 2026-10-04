package org.lovetropics.games.common.core.game.behavior.event;

import net.minecraft.util.context.ContextMap;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;

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
