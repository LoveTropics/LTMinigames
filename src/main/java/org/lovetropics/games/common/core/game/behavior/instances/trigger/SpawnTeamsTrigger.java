package org.lovetropics.games.common.core.game.behavior.instances.trigger;

import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorTypes;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamState;
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
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
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
