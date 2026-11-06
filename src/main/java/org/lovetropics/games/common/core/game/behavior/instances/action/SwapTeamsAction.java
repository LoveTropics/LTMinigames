package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.player.MutablePlayerSet;
import org.lovetropics.games.common.core.game.player.PlayerSet;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamState;

import java.util.List;

public record SwapTeamsAction(double distanceThreshold) implements IGameBehavior {
	public static final MapCodec<SwapTeamsAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.DOUBLE.optionalFieldOf("distance_threshold", Double.MAX_VALUE).forGetter(c -> c.distanceThreshold)
	).apply(i, SwapTeamsAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);

		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			if (game.participants().size() <= 1) {
				return false;
			}
			if (distanceThreshold == Double.MAX_VALUE) {
				shuffleTeams(game, teams);
			}
			return true;
		});
	}

	private void shuffleSpecificPlayers(IGamePhase game, List<ServerPlayer> players) {
		List<Vec3> playerPositions = players.stream()
				.map(Entity::position)
				.toList();

		for (int i = 0; i < players.size(); i++) {
			ServerPlayer player = players.get(i);
			Vec3 teleportTo = playerPositions.get((i + 1) % playerPositions.size());
			player.teleportTo(teleportTo.x, teleportTo.y, teleportTo.z);
		}
	}

	private void shuffleSpecificTeams(IGamePhase game, TeamState teamState, List<GameTeamKey> teams) {
		for (int i = 0; i < teams.size(); i++) {
			GameTeamKey team = teams.get(i);
			GameTeamKey otherTeam = teams.get((i + 1) % teams.size());
			PlayerSet firstTeamPlayers = teamState.getPlayersForTeam(game, team);
			PlayerSet secondTeamPlayers = teamState.getPlayersForTeam(game, otherTeam);
			MutablePlayerSet mutablePlayerSet = new MutablePlayerSet();
			mutablePlayerSet.addAll(firstTeamPlayers);
			mutablePlayerSet.addAll(secondTeamPlayers);
			shuffleSpecificPlayers(game, mutablePlayerSet.shuffledCopy(game.random()));
		}
	}

	private void shuffleTeams(IGamePhase game, TeamState teams) {
		List<GameTeamKey> shuffledTeams = teams.getTeamKeys().stream().toList();
		Util.shuffle(shuffledTeams, game.random());
		shuffleSpecificTeams(game, teams, shuffledTeams);
	}
}
