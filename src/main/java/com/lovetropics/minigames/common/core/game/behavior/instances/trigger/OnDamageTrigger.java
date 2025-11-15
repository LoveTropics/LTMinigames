package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.util.context.ContextMap;

public record OnDamageTrigger(GameActionList<ServerPlayer> actions) implements IGameBehavior {
	public static final MapCodec<OnDamageTrigger> CODEC = GameActionList.PLAYER_MAP_CODEC.xmap(OnDamageTrigger::new, OnDamageTrigger::actions);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		actions.register(game, events);

		events.listen(GamePlayerEvents.DAMAGE, (player, damageSource, amount) -> {
			actions.apply(game, ContextMap.EMPTY, player);
			return TriState.DEFAULT;
		});
	}
}
