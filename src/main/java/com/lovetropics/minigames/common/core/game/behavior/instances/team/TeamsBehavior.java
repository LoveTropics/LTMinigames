package com.lovetropics.minigames.common.core.game.behavior.instances.team;

import com.lovetropics.lib.permission.PermissionsApi;
import com.lovetropics.lib.permission.role.Role;
import com.lovetropics.lib.permission.role.RoleReader;
import com.lovetropics.minigames.common.content.MinigameTexts;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GameTeamEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.lovetropics.minigames.common.core.game.util.TeamAllocator;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.List;
import java.util.Map;

public final class TeamsBehavior implements IGameBehavior {
	public static final MapCodec<TeamsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.BOOL.optionalFieldOf("friendly_fire", false).forGetter(c -> c.friendlyFire),
			Codec.BOOL.optionalFieldOf("static_team_ids", false).forGetter(c -> c.staticTeamIds),
			Codec.BOOL.optionalFieldOf("player_collision", true).forGetter(c -> c.playerCollision)
	).apply(i, TeamsBehavior::new));

	private final Map<GameTeamKey, PlayerTeam> scoreboardTeams = new Object2ObjectOpenHashMap<>();

	private final boolean friendlyFire;
	private final boolean staticTeamIds;
	private final boolean playerCollision;

	private TeamState teams;

	public TeamsBehavior(boolean friendlyFire, boolean staticTeamIds, boolean playerCollision) {
		this.friendlyFire = friendlyFire;
		this.staticTeamIds = staticTeamIds;
		this.playerCollision = playerCollision;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		teams = game.instanceState().getOrThrow(TeamState.KEY);

		addTeamsToScoreboard(game);

		events.listen(GamePlayerEvents.BEFORE_ADD_PLAYERS, (participants, spectators) -> {
			teams.allocatePlayers(participants);
			game.invoker(GameTeamEvents.TEAMS_ALLOCATED).onTeamsAllocated();
		});

		events.listen(GamePlayerEvents.ADD, player -> {
			GameTeamKey teamKey = teams.getTeamForPlayer(player);
			if (teamKey != null) {
				GameTeam team = teams.getTeamByKey(teamKey);
				if (team != null) {
					applyTeamToPlayer(game, team, player);
				}
			}
		});

		events.listen(GamePhaseEvents.DESTROY, () -> onDestroy(game));
		events.listen(GamePlayerEvents.ALLOCATE_ROLES, this::reassignPlayerRoles);

		events.listen(GamePlayerEvents.LEAVE, player -> removePlayerFromTeams(game, player));
		events.listen(GamePlayerEvents.DAMAGE, this::onPlayerHurt);
		events.listen(GamePlayerEvents.ATTACK, this::onPlayerAttack);

		game.statistics().global().set(StatisticKey.TEAMS, true);
	}

	private void addTeamsToScoreboard(IGamePhase game) {
		MinecraftServer server = game.server();
		ServerScoreboard scoreboard = server.getScoreboard();

		for (GameTeam team : teams) {
			String teamId = createTeamId(team.key());
			PlayerTeam scoreboardTeam = getOrCreateScoreboardTeam(scoreboard, team, teamId);
			scoreboardTeams.put(team.key(), scoreboardTeam);
		}
	}

	private PlayerTeam getOrCreateScoreboardTeam(ServerScoreboard scoreboard, GameTeam team, String teamId) {
		PlayerTeam scoreboardTeam = scoreboard.getPlayerTeam(teamId);
		if (scoreboardTeam == null) {
			scoreboardTeam = scoreboard.addPlayerTeam(teamId);
		}

		scoreboardTeam.setDisplayName(team.config().name());
		scoreboardTeam.setColor(team.config().formatting());
		scoreboardTeam.setAllowFriendlyFire(friendlyFire);
		scoreboardTeam.setCollisionRule(playerCollision ? Team.CollisionRule.ALWAYS : Team.CollisionRule.NEVER);

		return scoreboardTeam;
	}

	private String createTeamId(GameTeamKey team) {
		if (staticTeamIds) {
			return team.id();
		} else {
			return team.id() + "_" + RandomStringUtils.randomAlphabetic(3);
		}
	}

	private void reassignPlayerRoles(TeamAllocator<PlayerRole, PlayerKey> allocator) {
		// All players that are assigned to a team should also be forced to be participating
		List<Role> assignedRoles = teams.assignedRoles();
		for (PlayerKey player : allocator.getKnownPlayers()) {
			RoleReader roles = PermissionsApi.lookup().byPlayerId(player.id());
			if (assignedRoles.stream().anyMatch(roles::has)) {
				allocator.addPlayer(player, PlayerRole.PARTICIPANT);
			}
		}
	}

	private void onDestroy(IGamePhase game) {
		ServerScoreboard scoreboard = game.server().getScoreboard();
		for (PlayerTeam team : scoreboardTeams.values()) {
			scoreboard.removePlayerTeam(team);
		}
	}

	private void applyTeamToPlayer(IGamePhase game, GameTeam team, ServerPlayer player) {
		game.invoker(GameTeamEvents.SET_GAME_TEAM).onSetGameTeam(player, teams, team.key());

		game.statistics().forPlayer(player).set(StatisticKey.TEAM, team.key());

		ServerScoreboard scoreboard = player.level().getScoreboard();
		PlayerTeam scoreboardTeam = scoreboardTeams.get(team.key());
		scoreboard.addPlayerToTeam(player.getScoreboardName(), scoreboardTeam);

		Component teamName = team.config().name().copy()
				.withStyle(ChatFormatting.BOLD, team.config().formatting());

		player.displayClientMessage(MinigameTexts.ON_TEAM.apply(teamName), false);
	}

	private void removePlayerFromTeams(IGamePhase game, ServerPlayer player) {
		final var teamKey = teams.removePlayer(player.getUUID());
		if (teamKey != null) {
			game.invoker(GameTeamEvents.REMOVE_FROM_TEAM).onRemoveFromTeam(player, teams, teamKey);
		}

		ServerScoreboard scoreboard = player.level().getScoreboard();
		scoreboard.removePlayerFromTeam(player.getScoreboardName());
	}

	private TriState onPlayerHurt(ServerPlayer player, DamageSource source, float amount) {
		if (!friendlyFire && teams.areSameTeam(source.getEntity(), player)) {
			return TriState.FALSE;
		}
		return TriState.DEFAULT;
	}

	private TriState onPlayerAttack(ServerPlayer player, Entity target) {
		if (!friendlyFire && teams.areSameTeam(player, target)) {
			return TriState.FALSE;
		}
		return TriState.DEFAULT;
	}
}
