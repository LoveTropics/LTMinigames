package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record SwapTeamsAction(
) implements IGameBehavior {
	public static final MapCodec<SwapTeamsAction> CODEC = MapCodec.unit(SwapTeamsAction::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);

		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			Map<GameTeamKey, List<LevelPos>> teamPlayerPositions = new HashMap<>();
			for (GameTeamKey team : targets.asTeams(game)) {
				List<LevelPos> positions = new ArrayList<>();
				for (ServerPlayer player : teams.getPlayersForTeam(game, team)) {
					positions.add(new LevelPos(player.level(), player.position()));
				}
				if (positions.isEmpty()) {
					continue;
				}
				Util.shuffle(positions, game.random());
				teamPlayerPositions.put(team, positions);
			}

			List<GameTeamKey> shuffledTeams = new ArrayList<>(teamPlayerPositions.keySet());
			if (shuffledTeams.size() < 2) {
				return false;
			}

			Util.shuffle(shuffledTeams, game.random());

			for (int i = 0; i < shuffledTeams.size(); i++) {
				GameTeamKey team = shuffledTeams.get(i);
				GameTeamKey otherTeam = shuffledTeams.get((i + 1) % shuffledTeams.size());

				List<ServerPlayer> teamPlayers = Lists.newArrayList(teams.getPlayersForTeam(game, team));
				List<LevelPos> targetPositions = teamPlayerPositions.get(otherTeam);

				for (int j = 0; j < teamPlayers.size(); j++) {
					ServerPlayer player = teamPlayers.get(j);
					LevelPos pos = targetPositions.get(j % targetPositions.size());
					player.teleportTo(pos.level, pos.pos.x, pos.pos.y, pos.pos.z, Set.of(), 0.0f, 0.0f, true);
				}
			}

			return true;
		});
	}

	private record LevelPos(ServerLevel level, Vec3 pos) {
	}
}
