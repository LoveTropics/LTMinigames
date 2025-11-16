package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextMap;

import java.util.function.Supplier;

public record SpawnTrigger(PlayerRole role, GameActionList action) implements IGameBehavior {
	public static final MapCodec<SpawnTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			PlayerRole.CODEC.fieldOf("role").forGetter(SpawnTrigger::role),
			GameActionList.MAP_CODEC.forGetter(SpawnTrigger::action)
	).apply(i, SpawnTrigger::new));

	@Override
	public void register(final IGamePhase game, final EventRegistrar events) throws GameException {
		action.register(game, events);
		events.listen(GamePlayerEvents.SPAWN, (playerId, spawn, role) -> {
			if (this.role == role) {
				spawn.run(player -> action.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player)));
			}
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.SPAWN;
	}
}
