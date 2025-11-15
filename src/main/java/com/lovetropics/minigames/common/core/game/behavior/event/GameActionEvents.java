package com.lovetropics.minigames.common.core.game.behavior.event;

import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.Entity;

// TODO: Enable any of these action targets to be reinterpreted in another context
public final class GameActionEvents {
	public static final GameEventType<Apply> APPLY = GameEventType.create(Apply.class, listeners -> context -> {
		boolean applied = false;
		for (Apply listener : listeners) {
			applied |= listener.apply(context);
		}
		return applied;
	});

	public static final GameEventType<ApplyToEntity> APPLY_TO_ENTITY = GameEventType.create(ApplyToEntity.class, listeners -> (context, target) -> {
		boolean applied = false;
		for (ApplyToEntity listener : listeners) {
			applied |= listener.apply(context, target);
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
		return type == APPLY || type == APPLY_TO_ENTITY || type == APPLY_TO_PLOT || type == APPLY_TO_PLAYER || type == APPLY_TO_TEAM;
	}

	public interface Apply {
		boolean apply(ContextMap context);
	}

	public interface ApplyToEntity {
		boolean apply(ContextMap context, Entity target);
	}

	public interface ApplyToPlayer {
		boolean apply(ContextMap context, ServerPlayer target);
	}

	public interface ApplyToPlot {
		boolean apply(ContextMap context, Plot plot);
	}

	public interface ApplyToTeam {
		boolean apply(ContextMap context, GameTeam team);
	}
}
