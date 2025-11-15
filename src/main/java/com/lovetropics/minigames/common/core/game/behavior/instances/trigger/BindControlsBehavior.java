package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.state.control.ControlCommand;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.Entity;

import java.util.Map;

public record BindControlsBehavior(Map<ControlCommand.Scope, Map<String, GameActionList<ServerPlayer>>> scopedActions) implements IGameBehavior {
	public static final MapCodec<BindControlsBehavior> CODEC = Codec.unboundedMap(ControlCommand.Scope.CODEC, Codec.unboundedMap(Codec.STRING, GameActionList.PLAYER_CODEC))
			.xmap(BindControlsBehavior::new, b -> b.scopedActions)
			.fieldOf("scopes");

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		scopedActions.forEach((scope, scopedActions) ->
				scopedActions.values().forEach(actions -> actions.register(game, events))
		);

		events.listen(GamePhaseEvents.REGISTER_COMMANDS, commands -> {
			scopedActions.forEach((scope, scopedActions) -> scopedActions.forEach((control, actions) -> {
				commands.register(control, scope, source -> {
					Entity entity = source.getEntity();
					if (entity instanceof ServerPlayer player) {
						actions.apply(game, ContextMap.EMPTY, player);
					} else {
						actions.apply(game, ContextMap.EMPTY);
					}
				});
			}));
		});
	}
}
