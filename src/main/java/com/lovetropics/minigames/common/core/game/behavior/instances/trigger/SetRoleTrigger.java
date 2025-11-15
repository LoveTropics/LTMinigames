package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextMap;

import java.util.function.Supplier;

public record SetRoleTrigger(PlayerRole role, GameActionList<ServerPlayer> action) implements IGameBehavior {
	public static final MapCodec<SetRoleTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			PlayerRole.CODEC.fieldOf("role").forGetter(SetRoleTrigger::role),
			GameActionList.PLAYER_CODEC.fieldOf("action").forGetter(SetRoleTrigger::action)
	).apply(i, SetRoleTrigger::new));

	@Override
	public void register(final IGamePhase game, final EventRegistrar events) throws GameException {
		action.register(game, events);
		events.listen(GamePlayerEvents.SET_ROLE, (player, role, lastRole) -> {
			if (this.role == role) {
				action.apply(game, ContextMap.EMPTY, player);
			}
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.SET_ROLE;
	}
}
