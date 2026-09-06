package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLogicEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.util.context.ContextMap;

import org.jspecify.annotations.Nullable;
import java.util.Map;
import java.util.function.Supplier;

// TODO: split up into separate trigger types
public record GeneralEventsTrigger(Map<String, GameActionList> eventActions) implements IGameBehavior {
	public static final MapCodec<GeneralEventsTrigger> CODEC = Codec.unboundedMap(Codec.STRING, GameActionList.CODEC)
			.xmap(GeneralEventsTrigger::new, b -> b.eventActions)
			.fieldOf("events");

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		for (var actions : eventActions.values()) {
			actions.register(game, events);
		}

		events.listen(GamePlayerEvents.JOIN, player -> invoke(game, "player_join", player));
		events.listen(GamePlayerEvents.LEAVE, player -> invoke(game, "player_leave", player));
		events.listen(GamePlayerEvents.ADD, player -> invoke(game, "player_add", player));
		events.listen(GamePlayerEvents.REMOVE, player -> invoke(game, "player_remove", player));

		events.listen(GamePlayerEvents.SET_ROLE, (player, role, lastRole) -> onPlayerSetRole(game, player, role, lastRole));

		events.listen(GamePlayerEvents.TICK, player -> invoke(game, "player_update", player));
		events.listen(GamePlayerEvents.RESPAWN, player -> invoke(game, "player_respawn", player));

		events.listen(GamePlayerEvents.DAMAGE, (player, damageSource, amount) -> {
			invoke(game, "player_hurt", player);
			return TriState.DEFAULT;
		});
		events.listen(GamePlayerEvents.ATTACK, (player, target) -> {
			invoke(game, "player_attack", player);
			return TriState.DEFAULT;
		});
		events.listen(GamePlayerEvents.DEATH, (player, damageSource) -> {
			invoke(game, "player_death", player);
			return TriState.DEFAULT;
		});

		events.listen(GameLogicEvents.GAME_OVER, winner -> invoke(game, "game_over"));

		events.listen(GamePhaseEvents.TICK, () -> invoke(game, "tick"));
	}

	private void onPlayerSetRole(IGamePhase game, ServerPlayer player, @Nullable PlayerRole role, @Nullable PlayerRole lastRole) {
		if (lastRole == PlayerRole.PARTICIPANT) {
			invoke(game, "player_stop_participate", player);
		} else if (lastRole == PlayerRole.SPECTATOR) {
			invoke(game, "player_stop_spectate", player);
		}

		if (role == PlayerRole.PARTICIPANT) {
			invoke(game, "player_participate", player);
		} else if (role == PlayerRole.SPECTATOR) {
			invoke(game, "player_spectate", player);
		}
	}

	private void invoke(IGamePhase game, String event) {
		var actions = eventActions.get(event);
		if (actions != null) {
			actions.apply(game, ContextMap.EMPTY);
		}
	}

	private void invoke(IGamePhase game, String event, ServerPlayer player) {
		var actions = eventActions.get(event);
		if (actions != null) {
			actions.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player));
		}
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.EVENTS;
	}
}
