package com.lovetropics.minigames.common.core.game.behavior.event;

import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContext;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import net.minecraft.server.level.ServerPlayer;

public final class GameActionEvents {
	public static final GameEventType<Apply> APPLY = GameEventType.create(Apply.class, listeners -> context -> {
		boolean applied = false;
		for (Apply listener : listeners) {
			applied |= listener.apply(context);
		}
		return applied;
	});

	public static final GameEventType<ApplyToPlayer> APPLY_TO_PLAYER = GameEventType.create(ApplyToPlayer.class, listeners -> (context, target) -> {
		boolean applied = false;
		for (ApplyToPlayer listener : listeners) {
			applied |= listener.apply(context, target);
		}
		return applied;
	});

	public static final GameEventType<ApplyToPlot> APPLY_TO_PLOT = GameEventType.create(ApplyToPlot.class, listeners -> (context, target) -> {
		boolean applied = false;
		for (ApplyToPlot listener : listeners) {
			applied |= listener.apply(context, target);
		}
		return applied;
	});

	public static final GameEventType<ApplyToTeam> APPLY_TO_TEAM = GameEventType.create(ApplyToTeam.class, listeners -> (context, team) -> {
		boolean applied = false;
		for (ApplyToTeam listener : listeners) {
			applied |= listener.apply(context, team);
		}
		return applied;
	});

	private GameActionEvents() {
	}

	public static boolean matches(GameEventType<?> type) {
		return type == APPLY || type == APPLY_TO_PLOT || type == APPLY_TO_PLAYER || type == APPLY_TO_TEAM;
	}

	public interface Apply {
		boolean apply(GameActionContext context);
	}

	public interface ApplyToPlayer {
		boolean apply(GameActionContext context, ServerPlayer target);
	}

	public interface ApplyToPlot {
		boolean apply(GameActionContext context, Plot plot);
	}

	public interface ApplyToTeam {
		boolean apply(GameActionContext context, GameTeam team);
	}
}
