package com.lovetropics.minigames.common.core.game.behavior.instances.team;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.MinigameTexts;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.config.BehaviorConfig;
import com.lovetropics.minigames.common.core.game.behavior.config.ConfigList;
import com.lovetropics.minigames.common.core.game.behavior.config.ConfigType;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.state.GameStateMap;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamConfig;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.lovetropics.minigames.common.core.game.util.SelectorItems;
import com.lovetropics.minigames.common.util.Scheduler;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Map;

public final class SetupTeamsBehavior implements IGameBehavior {
	private static final ResourceLocation CONFIG_ID = LoveTropics.location("team_list");
	private static final GameTeam DEFAULT_TEAM = new GameTeam(
			new GameTeamKey(""),
			new GameTeamConfig(CommonComponents.EMPTY, DyeColor.BLACK, ChatFormatting.BLACK, List.of(), 1)
	);
	private static final BehaviorConfig<List<GameTeam>> CFG_TEAMS = BehaviorConfig.fieldOf("teams", GameTeam.CODEC.listOf())
			.defaultInstanceHint("", DEFAULT_TEAM, GameTeam.CODEC)
			.listTypeHint("", ConfigType.COMPOSITE)
			.enumHint("[].dye", s -> DyeColor.byName(s, null))
			.enumHint("[].text", ChatFormatting::getByName);

	public static final MapCodec<SetupTeamsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			CFG_TEAMS.forGetter(c -> c.teams)
	).apply(i, SetupTeamsBehavior::new));

	private final List<GameTeam> teams;

	private TeamState teamState;
	private SelectorItems<GameTeam> selectors;

	public SetupTeamsBehavior(List<GameTeam> teams) {
		this.teams = teams;
	}

	@Override
	public ConfigList getConfigurables() {
		return ConfigList.builder(CONFIG_ID)
				.with(CFG_TEAMS, teams)
				.build();
	}

	@Override
	public IGameBehavior configure(Map<ResourceLocation, ConfigList> configs) {
		return new SetupTeamsBehavior(CFG_TEAMS.getValue(configs.get(CONFIG_ID)));
	}

	@Override
	public void registerState(IGamePhase game, GameStateMap phaseState, GameStateMap instanceState) {
		teamState = instanceState.register(TeamState.KEY, new TeamState(teams));
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GamePlayerEvents.ADD, player -> onPlayerWaiting(game, player));

		SelectorItems.Handlers<GameTeam> handlers = new SelectorItems.Handlers<>() {
            @Override
            public void onPlayerSelected(ServerPlayer player, GameTeam team) {
                onRequestJoinTeam(player, team);
            }

            @Override
            public String getIdFor(GameTeam team) {
                return team.key().id();
            }

            @Override
            public Component getNameFor(GameTeam team) {
                return MinigameTexts.JOIN_TEAM.apply(team.config().name()).withStyle(team.config().formatting());
            }

            @Override
            public Item getItemFor(GameTeam team) {
				return switch (team.config().dye()) {
					case WHITE -> Items.WHITE_WOOL;
					case ORANGE -> Items.ORANGE_WOOL;
					case MAGENTA -> Items.MAGENTA_WOOL;
					case LIGHT_BLUE -> Items.LIGHT_BLUE_WOOL;
					case YELLOW -> Items.YELLOW_WOOL;
					case LIME -> Items.LIME_WOOL;
					case PINK -> Items.PINK_WOOL;
					case GRAY -> Items.GRAY_WOOL;
					case LIGHT_GRAY -> Items.LIGHT_GRAY_WOOL;
					case CYAN -> Items.CYAN_WOOL;
					case PURPLE -> Items.PURPLE_WOOL;
					case BLUE -> Items.BLUE_WOOL;
					case BROWN -> Items.BROWN_WOOL;
					case GREEN -> Items.GREEN_WOOL;
					case RED -> Items.RED_WOOL;
					case BLACK -> Items.BLACK_WOOL;
				};
            }
        };

		selectors = new SelectorItems<>(handlers, teams.toArray(new GameTeam[0]));
		selectors.applyTo(events);
	}

	private void onPlayerWaiting(IGamePhase game, ServerPlayer player) {
		PlayerRole forcedRole = game.lobby().getPlayers().getForcedRoleFor(player);
		if (forcedRole != PlayerRole.SPECTATOR && teamState.getPollingTeams().size() > 1) {
			for (Component message : MinigameTexts.TEAMS_INTRO) {
				player.displayClientMessage(message, false);
			}

			Scheduler.nextTick().run(server -> {
				selectors.giveSelectorsTo(player);
			});
		}
	}

	private void onRequestJoinTeam(ServerPlayer player, GameTeam team) {
		teamState.setPlayerPreference(player.getUUID(), team.key());

		Component teamName = team.config().name().copy().withStyle(team.config().formatting(), ChatFormatting.BOLD);
		player.displayClientMessage(MinigameTexts.JOINED_TEAM.apply(teamName), false);
	}
}
