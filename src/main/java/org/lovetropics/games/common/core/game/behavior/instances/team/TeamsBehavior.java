package org.lovetropics.games.common.core.game.behavior.instances.team;

import com.lovetropics.lib.permission.PermissionsApi;
import com.lovetropics.lib.permission.role.Role;
import com.lovetropics.lib.permission.role.RoleReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.content.MinigameTexts;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.behavior.event.GameTeamEvents;
import org.lovetropics.games.common.core.game.command.GameCommandRegistrar;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.core.game.state.GameStateMap;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.game.state.team.GameTeam;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamSetupState;
import org.lovetropics.games.common.core.game.state.team.TeamState;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class TeamsBehavior implements IGameBehavior {
	public static final MapCodec<TeamsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ScoreboardTeams.CODEC.forGetter(t -> t.scoreboardTeams),
			Codec.unboundedMap(GameTeamKey.CODEC, SetupTeamsBehavior.TeamConfig.CODEC).optionalFieldOf("instant_teams", Map.of()).forGetter(t -> t.instantTeams)
	).apply(i, TeamsBehavior::new));

	private static final DynamicCommandExceptionType NO_TEAM = new DynamicCommandExceptionType(team -> Component.literal("No team exists with id: " + team));

	private static final Logger LOGGER = LogUtils.getLogger();

	private final ScoreboardTeams scoreboardTeams;
	private final Map<GameTeamKey, SetupTeamsBehavior.TeamConfig> instantTeams;

	private @Nullable TeamSetupState teamSetup;
	private TeamState teams;

	public TeamsBehavior(ScoreboardTeams scoreboardTeams, Map<GameTeamKey, SetupTeamsBehavior.TeamConfig> instantTeams) {
		this.scoreboardTeams = scoreboardTeams;
		this.instantTeams = instantTeams;
	}

	@Override
	public void registerState(IGamePhase game, GameStateMap phaseState, GameStateMap instanceState) {
		TeamState existingTeams = instanceState.getOrNull(TeamState.KEY);
		// We might run this behavior after teams have already been set up (for example in a sub-game), and we don't want to set it up twice
		if (existingTeams == null) {
			teams = setupTeams(game, instanceState);
			instanceState.register(TeamState.KEY, teams);
		} else {
			teams = existingTeams;
		}
	}

	private TeamState setupTeams(IGamePhase game, GameStateMap instanceState) {
		if (!instantTeams.isEmpty()) {
			return new TeamState(game, instantTeams.entrySet().stream().map(entry -> {
				SetupTeamsBehavior.TeamConfig config = entry.getValue();
				return new GameTeam(entry.getKey(), config.dyeColor(), config.name());
			}).toList());
		}

		teamSetup = instanceState.getOrThrow(TeamSetupState.KEY);
		return teamSetup.createInitialTeamState(game);
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		teams.register(events);

		TeamSetupState teamSetup = this.teamSetup;
		if (teamSetup != null) {
			events.listen(GamePlayerEvents.BEFORE_ADD_PLAYERS, (participants, spectators) ->
					teamSetup.allocatePlayers(teams, participants)
			);
			events.listen(GamePlayerEvents.ALLOCATE_ROLES, allocator -> {
				// All players that are assigned to a team should also be forced to be participating
				teamSetup.assignedPlayers().forEach(player ->
						allocator.addPlayer(player, PlayerRole.PARTICIPANT)
				);
			});
			this.teamSetup = null;
		}

		if (!instantTeams.isEmpty()) {
			Map<Role, GameTeamKey> roleToTeams = SetupTeamsBehavior.buildRoleToTeamMap(instantTeams);
			events.listen(GamePlayerEvents.ADD, player -> {
				RoleReader roles = PermissionsApi.lookup().byPlayer(player);
				for (Map.Entry<Role, GameTeamKey> entry : roleToTeams.entrySet()) {
					if (roles.has(entry.getKey())) {
						teams.addPlayerTo(PlayerKey.from(player), entry.getValue());
						LOGGER.debug("Assigning {} to {} based on role assignments", player.getPlainTextName(), entry.getKey());
						break;
					}
				}
			});
		}

		events.listen(GamePlayerEvents.BEFORE_ADD_PLAYERS, (participants, spectators) -> {
			Map<PlayerKey, GameTeamKey> participantTeams = new HashMap<>();
			for (PlayerKey participant : participants) {
				GameTeamKey team = teams.getTeamForPlayer(participant);
				if (team != null) {
					participantTeams.put(participant, team);
				}
			}
			game.invoker(GameTeamEvents.TEAMS_ALLOCATED).onTeamsAllocated(participantTeams);
		});

		events.listen(GameTeamEvents.SET_GAME_TEAM, (player, _, team) -> {
			game.statistics().forPlayer(player).set(StatisticKey.TEAM, team.key());

			Component teamName = team.styledName().withStyle(ChatFormatting.BOLD);
			player.sendSystemMessage(MinigameTexts.ON_TEAM.apply(teamName), false);
		});

		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, _) -> registerCommands(commands));

		game.statistics().global().set(StatisticKey.TEAMS, true);

		scoreboardTeams.register(game, events);
	}

	private void registerCommands(GameCommandRegistrar commands) {
		commands.register(Commands.literal("team")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("assign")
						.then(Commands.argument("player", GameProfileArgument.gameProfile())
								.then(Commands.argument("team", StringArgumentType.string())
										.suggests(suggestTeam(teams))
										.executes(context -> {
											Collection<NameAndId> players = GameProfileArgument.getGameProfiles(context, "player");
											GameTeamKey team = getTeamArgument(context, "team", teams).key();
											for (NameAndId player : players) {
												teams.addPlayerTo(PlayerKey.from(player), team);
											}
											context.getSource().sendSuccess(() -> Component.literal("Assigned " + players.size() + " players to " + team.id()), true);

											return players.size();
										})
								)
						)
				)
				.then(Commands.literal("clear")
						.then(Commands.argument("player", GameProfileArgument.gameProfile())
								.executes(context -> {
									Collection<NameAndId> players = GameProfileArgument.getGameProfiles(context, "player");
									for (NameAndId player : players) {
										teams.removePlayer(player.id());
									}
									context.getSource().sendSuccess(() -> Component.literal("Cleared " + players.size() + " players"), true);
									return players.size();
								})
						)
				)
		);
	}

	private static GameTeam getTeamArgument(CommandContext<CommandSourceStack> context, String name, TeamState teams) throws CommandSyntaxException {
		String teamId = StringArgumentType.getString(context, name);
		return teams.stream()
				.filter(team -> team.key().id().equals(teamId))
				.findFirst()
				.orElseThrow(() -> NO_TEAM.create(teamId));
	}

	private static SuggestionProvider<CommandSourceStack> suggestTeam(TeamState teams) {
		return (_, builder) ->
				SharedSuggestionProvider.suggest(teams.stream().map(i -> i.key().id()), builder);
	}
}
