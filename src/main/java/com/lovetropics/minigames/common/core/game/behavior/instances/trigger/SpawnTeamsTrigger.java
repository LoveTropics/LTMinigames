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
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.context.ContextMap;

import java.util.Map;
import java.util.function.Supplier;

public record SpawnTeamsTrigger(PlayerRole role, Map<GameTeamKey, GameActionList> teamActions) implements IGameBehavior {
	public static final MapCodec<SpawnTeamsTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			PlayerRole.CODEC.fieldOf("role").forGetter(SpawnTeamsTrigger::role),
			Codec.unboundedMap(GameTeamKey.CODEC, GameActionList.CODEC).optionalFieldOf("actions", Map.of()).forGetter(SpawnTeamsTrigger::teamActions)
	).apply(i, SpawnTeamsTrigger::new));

	@Override
	public void register(final IGamePhase game, final EventRegistrar events) throws GameException {
		TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);
		for (GameActionList actions : teamActions.values()) {
			actions.register(game, events);
		}
		events.listen(GamePlayerEvents.SPAWN, (playerId, spawn, role) -> {
			if (this.role == role) {
				spawn.run(player -> {
					GameTeamKey teamForPlayer = teams.getTeamForPlayer(player);
					GameActionList actions = teamActions.get(teamForPlayer);
					if (actions != null) {
						actions.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player));
					}
				});
			}
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.SPAWN;
	}
}
