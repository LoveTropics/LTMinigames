package org.lovetropics.games.common.core.game.behavior.instances.team;

import com.lovetropics.lib.permission.PermissionsApi;
import com.lovetropics.lib.permission.role.Role;
import com.lovetropics.lib.permission.role.RoleReader;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.lovetropics.games.common.content.MinigameTexts;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.command.GameCommandRegistrar;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;
import org.lovetropics.games.common.core.game.state.team.GameTeam;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamSetupState;
import org.lovetropics.games.common.core.game.util.SelectorItems;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SetupTeamsBehavior(
		Map<GameTeamKey, TeamConfig> initialTeams
) implements IGameBehavior {
	public static final MapCodec<SetupTeamsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.unboundedMap(GameTeamKey.CODEC, TeamConfig.CODEC).fieldOf("teams").forGetter(SetupTeamsBehavior::initialTeams)
	).apply(i, SetupTeamsBehavior::new));

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final DynamicCommandExceptionType NO_TEAM = new DynamicCommandExceptionType(team -> Component.literal("No team exists with id: " + team));
	private static final DynamicCommandExceptionType NO_COLOR = new DynamicCommandExceptionType(color -> Component.literal("No color exists with id: " + color));
	private static final DynamicCommandExceptionType TEAM_ALREADY_EXISTS = new DynamicCommandExceptionType(team -> Component.literal("Team already exists with id: " + team));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		TeamSetupState teamState = game.instanceState().register(TeamSetupState.KEY, new TeamSetupState());
		for (Map.Entry<GameTeamKey, TeamConfig> entry : initialTeams.entrySet()) {
			TeamConfig config = entry.getValue();
			TeamSetupState.Instance instance = teamState.addTeam(new GameTeam(entry.getKey(), config.dyeColor(), config.name()));
			instance.setMaxPlayers(config.maxSize);
			instance.setOpenToJoin(config.assignedRoles.isEmpty());
		}

		Map<Role, GameTeamKey> roleToTeams = buildRoleToTeamMap();
		events.listen(GamePlayerEvents.ADD, player -> {
			RoleReader roles = PermissionsApi.lookup().byPlayer(player);
			for (Map.Entry<Role, GameTeamKey> entry : roleToTeams.entrySet()) {
				if (roles.has(entry.getKey())) {
					teamState.assignPlayer(PlayerKey.from(player), entry.getValue());
					LOGGER.debug("Assigning {} to {} based on role assignments", player.getPlainTextName(), entry.getKey());
					break;
				}
			}
		});
		events.listen(GamePlayerEvents.LEAVE, player -> teamState.removePlayer(PlayerKey.from(player)));

		SelectorItems<TeamSetupState.Instance> selectors = setupSelector(events, teamState);
		resetSelectors(teamState, selectors);

		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) ->
				registerCommands(commands, buildContext, teamState, selectors)
		);
	}

	private static void resetSelectors(TeamSetupState teamState, SelectorItems<TeamSetupState.Instance> selectors) {
		List<TeamSetupState.Instance> openTeams = teamState.teamsStream()
				.filter(TeamSetupState.Instance::isOpenToJoin)
				.toList();
		if (openTeams.size() > 1) {
			selectors.set(openTeams);
		} else {
			selectors.set(List.of());
		}
	}

	private void registerCommands(GameCommandRegistrar commands, CommandBuildContext buildContext, TeamSetupState teamState, SelectorItems<TeamSetupState.Instance> selectors) {
		commands.register(Commands.literal("team")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("add")
						.then(Commands.argument("team", StringArgumentType.string())
								.then(Commands.argument("name", ComponentArgument.textComponent(buildContext))
										.then(Commands.argument("color", StringArgumentType.string())
												.suggests((_, builder) ->
														SharedSuggestionProvider.suggest(DyeColor.VALUES.stream().map(DyeColor::getSerializedName), builder)
												)
												.executes(context -> {
													GameTeamKey key = new GameTeamKey(StringArgumentType.getString(context, "team"));
													if (teamState.getTeam(key) != null) {
														throw TEAM_ALREADY_EXISTS.create(key.id());
													}
													Component name = ComponentArgument.getResolvedComponent(context, "name");
													String colorId = StringArgumentType.getString(context, "color");
													DyeColor color = DyeColor.CODEC.byName(colorId);
													if (color == null) {
														throw NO_COLOR.create(colorId);
													}
													TeamSetupState.Instance instance = teamState.addTeam(new GameTeam(key, color, name));
													instance.setOpenToJoin(false);
													resetSelectors(teamState, selectors);
													context.getSource().sendSuccess(
															() -> Component.translatable("Added %s team, use %s to make it public",
																	key.id(),
																	Component.literal("/game team open").withStyle(ChatFormatting.GRAY)
															),
															true
													);
													return 1;
												})
										)
								)
						)
				)
				.then(Commands.literal("remove")
						.then(Commands.argument("team", StringArgumentType.string())
								.suggests(suggestTeam(teamState))
								.executes(context -> {
									TeamSetupState.Instance team = getTeamArgument(context, "team", teamState);
									teamState.removeTeam(team.key());
									resetSelectors(teamState, selectors);
									context.getSource().sendSuccess(() -> Component.translatable("Removed %s team", team.team().styledName()), true);
									return 1;
								})
						)
				)
				.then(Commands.literal("open")
						.then(Commands.argument("team", StringArgumentType.string())
								.suggests(suggestTeam(teamState))
								.executes(context -> {
									TeamSetupState.Instance team = getTeamArgument(context, "team", teamState);
									team.setOpenToJoin(true);
									resetSelectors(teamState, selectors);
									context.getSource().sendSuccess(() -> Component.translatable("Opened %s for public joins", team.team().styledName()), true);
									return 1;
								})
						)
				)
				.then(Commands.literal("close")
						.then(Commands.argument("team", StringArgumentType.string())
								.suggests(suggestTeam(teamState))
								.executes(context -> {
									TeamSetupState.Instance team = getTeamArgument(context, "team", teamState);
									team.setOpenToJoin(false);
									resetSelectors(teamState, selectors);
									context.getSource().sendSuccess(() -> Component.translatable("Closed %s for public joins", team.team().styledName()), true);
									return 1;
								})
						)
				)
				.then(Commands.literal("maxsize")
						.then(Commands.argument("team", StringArgumentType.string())
								.suggests(suggestTeam(teamState))
								.then(Commands.argument("size", IntegerArgumentType.integer(1))
										.executes(context -> {
											TeamSetupState.Instance team = getTeamArgument(context, "team", teamState);
											int maxSize = IntegerArgumentType.getInteger(context, "size");
											team.setMaxPlayers(maxSize);
											context.getSource().sendSuccess(() -> Component.translatable("Set maximum size of %s to %s", team.team().styledName(), maxSize), true);
											return 1;
										})
								)
						)
				)
				.then(Commands.literal("assign")
						.then(Commands.argument("player", GameProfileArgument.gameProfile())
								.then(Commands.argument("team", StringArgumentType.string())
										.suggests(suggestTeam(teamState))
										.executes(context -> {
											Collection<NameAndId> players = GameProfileArgument.getGameProfiles(context, "player");
											GameTeamKey team = getTeamArgument(context, "team", teamState).key();
											for (NameAndId player : players) {
												teamState.assignPlayer(PlayerKey.from(player), team);
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
										teamState.removePlayer(PlayerKey.from(player));
									}
									context.getSource().sendSuccess(() -> Component.literal("Cleared " + players.size() + " players"), true);
									return players.size();
								})
						)
				)
				.then(Commands.literal("list")
						.executes(context -> {
							CommandSourceStack source = context.getSource();
							teamState.teamsStream().forEach(team -> {
								source.sendSystemMessage(team.team().styledName());
								List<PlayerKey> playersAssigned = teamState.playersAssignedTo(team.key()).toList();
								for (PlayerKey player : playersAssigned) {
									source.sendSystemMessage(Component.literal(" - ").append(player.name()).append(" (assigned)"));
								}
								List<PlayerKey> playersWithPreference = teamState.playersWithPreferenceFor(team.key())
										.filter(player -> !playersAssigned.contains(player))
										.toList();
								for (PlayerKey player : playersWithPreference) {
									source.sendSystemMessage(Component.literal(" - ").append(player.name()).append(" (preference)"));
								}
							});
							return 1;
						})
				)
		);
	}

	private static TeamSetupState.Instance getTeamArgument(CommandContext<CommandSourceStack> context, String name, TeamSetupState teams) throws CommandSyntaxException {
		String teamId = StringArgumentType.getString(context, name);
		return teams.teamsStream()
				.filter(team -> team.key().id().equals(teamId))
				.findFirst()
				.orElseThrow(() -> NO_TEAM.create(teamId));
	}

	private static SuggestionProvider<CommandSourceStack> suggestTeam(TeamSetupState teams) {
		return (_, builder) ->
				SharedSuggestionProvider.suggest(teams.teamsStream().map(i -> i.key().id()), builder);
	}

	private Map<Role, GameTeamKey> buildRoleToTeamMap() {
		// Preserve order: first-specified role matches
		Map<Role, GameTeamKey> assignedRoles = new LinkedHashMap<>();
		for (Map.Entry<GameTeamKey, TeamConfig> entry : initialTeams.entrySet()) {
			for (String roleId : entry.getValue().assignedRoles()) {
				Role role = PermissionsApi.provider().get(roleId);
				if (role != null) {
					assignedRoles.putIfAbsent(role, entry.getKey());
				} else {
					LOGGER.warn("Could not find role with id {}, will not assign", roleId);
				}
			}
		}
		return assignedRoles;
	}

	private SelectorItems<TeamSetupState.Instance> setupSelector(EventRegistrar events, TeamSetupState teamState) {
		SelectorItems.Handlers<TeamSetupState.Instance> handlers = new SelectorItems.Handlers<>() {
			@Override
			public void onPlayerSelected(ServerPlayer player, TeamSetupState.Instance team) {
				teamState.setPlayerPreference(player, team.key());

				Component teamName = team.team().styledName().withStyle(ChatFormatting.BOLD);
				player.sendSystemMessage(MinigameTexts.JOINED_TEAM.apply(teamName), false);
			}

			@Override
			public String getIdFor(TeamSetupState.Instance team) {
				return team.key().id();
			}

			@Override
			public Component getNameFor(TeamSetupState.Instance team) {
				return MinigameTexts.JOIN_TEAM.apply(team.team().styledName());
			}

			@Override
			public Item getItemFor(TeamSetupState.Instance team) {
				return Items.WOOL.pick(team.team().dyeColor());
			}
		};

		SelectorItems<TeamSetupState.Instance> selectors = new SelectorItems<>(handlers, List.of());
		selectors.applyTo(events);

		events.listen(GamePlayerEvents.SPAWN, (_, spawn, _) -> spawn.run(player -> {
			for (Component message : MinigameTexts.TEAMS_INTRO) {
				player.sendSystemMessage(message, false);
			}
			selectors.giveSelectorsTo(player);
		}));

		return selectors;
	}

	public record TeamConfig(
			Component name,
			DyeColor dyeColor,
			List<String> assignedRoles,
			int maxSize
	) {
		public static final Codec<TeamConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
				ComponentSerialization.CODEC.fieldOf("name").forGetter(TeamConfig::name),
				ExtraCodecs.optionalAlwaysPresentFieldOf(DyeColor.CODEC, "dye", DyeColor.WHITE).forGetter(TeamConfig::dyeColor),
				ExtraCodecs.optionalAlwaysPresentFieldOf(Codec.STRING.listOf(), "assign_roles", List.of()).forGetter(TeamConfig::assignedRoles),
				ExtraCodecs.optionalAlwaysPresentFieldOf(ExtraCodecs.POSITIVE_INT, "max_size", Integer.MAX_VALUE).forGetter(TeamConfig::maxSize)
		).apply(i, TeamConfig::new));

		public MutableComponent styledName() {
			return name.copy().withColor(GameTeam.teamColor(dyeColor).textColor());
		}
	}
}
