package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamState;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public record TeamDispatchAction(
		Map<GameTeamKey, GameActionList> byTeam
) implements IGameBehavior {
	public static final MapCodec<TeamDispatchAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.unboundedMap(GameTeamKey.CODEC, GameActionList.CODEC).fieldOf("by_team").forGetter(TeamDispatchAction::byTeam)
	).apply(i, TeamDispatchAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);
		for (GameActionList actions : byTeam.values()) {
			actions.register(game, events);
		}
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			List<GameTeamKey> targetTeams = targets.asTeams(game);
			if (targetTeams.isEmpty()) {
				return false;
			} else if (targetTeams.size() == 1) {
				// Only applying to one team, pass the targets through verbatim
				GameActionList actions = byTeam.get(targetTeams.getFirst());
				return actions != null && actions.apply(game, context, targets);
			}
			// Multiple teams! We need to split, the best chance we have is downgrading to players
			Multimap<GameTeamKey, ServerPlayer> splitTargets = HashMultimap.create();
			for (ServerPlayer player : targets.asPlayers(game)) {
				GameTeamKey team = teams.getTeamForPlayer(player);
				if (team != null) {
					splitTargets.put(team, player);
				}
			}
			boolean applied = false;
			for (Map.Entry<GameTeamKey, Collection<ServerPlayer>> entry : splitTargets.asMap().entrySet()) {
				GameActionList actions = byTeam.get(entry.getKey());
				if (actions != null) {
					ActionSubjects<?> teamTargets = ActionSubjects.ofPlayers(List.copyOf(entry.getValue()))
							.coerceInto(game, targets.type());
					applied |= actions.apply(game, context, teamTargets);
				}
			}
			return applied;
		});
	}
}
