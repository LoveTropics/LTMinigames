package org.lovetropics.games.common.core.game.behavior.instances.team;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.apache.commons.lang3.RandomStringUtils;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.behavior.event.GameTeamEvents;
import org.lovetropics.games.common.core.game.state.team.GameTeam;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamState;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record ScoreboardTeams(
		boolean friendlyFire,
		boolean staticTeamIds,
		boolean playerCollision
) implements IGameBehavior {
	public static final MapCodec<ScoreboardTeams> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.BOOL.optionalFieldOf("friendly_fire", false).forGetter(c -> c.friendlyFire),
			Codec.BOOL.optionalFieldOf("static_team_ids", false).forGetter(c -> c.staticTeamIds),
			Codec.BOOL.optionalFieldOf("player_collision", true).forGetter(c -> c.playerCollision)
	).apply(i, ScoreboardTeams::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);
		ServerScoreboard scoreboard = game.server().getScoreboard();

		Map<GameTeamKey, PlayerTeam> scoreboardTeams = new HashMap<>();

		events.listen(GamePhaseEvents.DESTROY, () -> {
			for (PlayerTeam team : scoreboardTeams.values()) {
				scoreboard.removePlayerTeam(team);
			}
		});

		events.listen(GameTeamEvents.SET_GAME_TEAM, (player, _, team) -> {
			PlayerTeam scoreboardTeam = scoreboardTeams.computeIfAbsent(team.key(), _ -> getOrCreateScoreboardTeam(scoreboard, team));
			scoreboard.addPlayerToTeam(player.getScoreboardName(), scoreboardTeam);
		});
		events.listen(GameTeamEvents.REMOVE_FROM_TEAM, (player, _, _) ->
				scoreboard.removePlayerFromTeam(player.getScoreboardName())
		);

		events.listen(GamePlayerEvents.DAMAGE, (player, source, _) -> filterDamage(teams, source.getEntity(), player));
		events.listen(GamePlayerEvents.ATTACK, (player, target) -> filterDamage(teams, player, target));
	}

	private PlayerTeam getOrCreateScoreboardTeam(ServerScoreboard scoreboard, GameTeam team) {
		String teamId = createTeamId(team.key());
		PlayerTeam scoreboardTeam = scoreboard.getPlayerTeam(teamId);
		if (scoreboardTeam == null) {
			scoreboardTeam = scoreboard.addPlayerTeam(teamId);
		}

		scoreboardTeam.setDisplayName(team.plainName());
		scoreboardTeam.setColor(Optional.of(team.teamColor()));
		scoreboardTeam.setAllowFriendlyFire(friendlyFire);
		scoreboardTeam.setCollisionRule(playerCollision ? Team.CollisionRule.ALWAYS : Team.CollisionRule.NEVER);

		return scoreboardTeam;
	}

	private String createTeamId(GameTeamKey team) {
		if (staticTeamIds) {
			return team.id();
		} else {
			return team.id() + "_" + RandomStringUtils.insecure().nextAlphabetic(3);
		}
	}

	private TriState filterDamage(TeamState teams, @Nullable Entity source, @Nullable Entity target) {
		if (!friendlyFire && teams.areSameTeam(source, target)) {
			return TriState.FALSE;
		}
		return TriState.DEFAULT;
	}
}
