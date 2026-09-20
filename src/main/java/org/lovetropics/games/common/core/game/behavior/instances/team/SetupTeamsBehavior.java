package org.lovetropics.games.common.core.game.behavior.instances.team;

import com.lovetropics.lib.permission.PermissionsApi;
import com.lovetropics.lib.permission.role.Role;
import com.lovetropics.lib.permission.role.RoleReader;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.lovetropics.games.common.content.MinigameTexts;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.state.team.GameTeam;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamSetupState;
import org.lovetropics.games.common.core.game.util.SelectorItems;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.lovetropics.lib.codec.MoreCodecs.inputOptionalFieldOf;

public record SetupTeamsBehavior(
		Map<GameTeamKey, TeamConfig> teams
) implements IGameBehavior {
	public static final MapCodec<SetupTeamsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.unboundedMap(GameTeamKey.CODEC, TeamConfig.CODEC).fieldOf("teams").forGetter(SetupTeamsBehavior::teams)
	).apply(i, SetupTeamsBehavior::new));

	private static final Logger LOGGER = LogUtils.getLogger();

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		TeamSetupState teamState = game.instanceState().register(TeamSetupState.KEY, new TeamSetupState());
		for (Map.Entry<GameTeamKey, TeamConfig> entry : teams.entrySet()) {
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
					teamState.assignPlayer(player, entry.getValue());
					LOGGER.debug("Assigning {} to {} based on role assignments", player.getPlainTextName(), entry.getKey());
					break;
				}
			}
		});
		events.listen(GamePlayerEvents.REMOVE, teamState::removePlayer);

		List<Map.Entry<GameTeamKey, TeamConfig>> openTeams = teams.entrySet().stream()
				.filter(entry -> entry.getValue().assignedRoles.isEmpty())
				.toList();
		if (openTeams.size() > 1) {
			setupSelector(events, teamState, openTeams);
		}
	}

	private Map<Role, GameTeamKey> buildRoleToTeamMap() {
		// Preserve order: first-specified role matches
		Map<Role, GameTeamKey> assignedRoles = new LinkedHashMap<>();
		for (Map.Entry<GameTeamKey, TeamConfig> entry : teams.entrySet()) {
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

	private void setupSelector(EventRegistrar events, TeamSetupState teamState, List<Map.Entry<GameTeamKey, TeamConfig>> openTeams) {
		SelectorItems.Handlers<Map.Entry<GameTeamKey, TeamConfig>> handlers = new SelectorItems.Handlers<>() {
			@Override
			public void onPlayerSelected(ServerPlayer player, Map.Entry<GameTeamKey, TeamConfig> team) {
				teamState.setPlayerPreference(player, team.getKey());

				Component teamName = team.getValue().styledName().withStyle(ChatFormatting.BOLD);
				player.sendSystemMessage(MinigameTexts.JOINED_TEAM.apply(teamName), false);
			}

			@Override
			public String getIdFor(Map.Entry<GameTeamKey, TeamConfig> team) {
				return team.getKey().id();
			}

			@Override
			public Component getNameFor(Map.Entry<GameTeamKey, TeamConfig> team) {
				return MinigameTexts.JOIN_TEAM.apply(team.getValue().styledName());
			}

			@Override
			public Item getItemFor(Map.Entry<GameTeamKey, TeamConfig> team) {
				return Items.WOOL.pick(team.getValue().dyeColor());
			}
		};

		SelectorItems<Map.Entry<GameTeamKey, TeamConfig>> selectors = new SelectorItems<>(handlers, openTeams);
		selectors.applyTo(events);

		events.listen(GamePlayerEvents.SPAWN, (_, spawn, _) -> spawn.run(player -> {
			for (Component message : MinigameTexts.TEAMS_INTRO) {
				player.sendSystemMessage(message, false);
			}
			selectors.giveSelectorsTo(player);
		}));
	}

	public record TeamConfig(
			Component name,
			DyeColor dyeColor,
			List<String> assignedRoles,
			int maxSize
	) {
		public static final Codec<TeamConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
				ComponentSerialization.CODEC.fieldOf("name").forGetter(TeamConfig::name),
				inputOptionalFieldOf(DyeColor.CODEC, "dye", DyeColor.WHITE).forGetter(TeamConfig::dyeColor),
				inputOptionalFieldOf(Codec.STRING.listOf(), "assign_roles", List.of()).forGetter(TeamConfig::assignedRoles),
				inputOptionalFieldOf(ExtraCodecs.POSITIVE_INT, "max_size", Integer.MAX_VALUE).forGetter(TeamConfig::maxSize)
		).apply(i, TeamConfig::new));

		public MutableComponent styledName() {
			return name.copy().withColor(GameTeam.teamColor(dyeColor).textColor());
		}
	}
}
