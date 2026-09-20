package org.lovetropics.games.common.core.game.behavior.instances.team;

import org.lovetropics.games.common.content.MinigameTexts;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.state.GameStateMap;
import org.lovetropics.games.common.core.game.state.team.GameTeam;
import org.lovetropics.games.common.core.game.state.team.TeamState;
import org.lovetropics.games.common.core.game.util.SelectorItems;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

public final class SetupTeamsBehavior implements IGameBehavior {
	public static final MapCodec<SetupTeamsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			GameTeam.CODEC.listOf().fieldOf("teams").forGetter(c -> c.teams)
	).apply(i, SetupTeamsBehavior::new));

	private final List<GameTeam> teams;

	private TeamState teamState;
	private SelectorItems<GameTeam> selectors;

	public SetupTeamsBehavior(List<GameTeam> teams) {
		this.teams = teams;
	}

	@Override
	public void registerState(IGamePhase game, GameStateMap phaseState, GameStateMap instanceState) {
		teamState = instanceState.register(TeamState.KEY, new TeamState(teams));
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
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
				return MinigameTexts.JOIN_TEAM.apply(team.config().name()).withColor(team.config().textColor());
			}

			@Override
			public Item getItemFor(GameTeam team) {
				return Items.WOOL.pick(team.config().dyeColor());
			}
		};

		selectors = new SelectorItems<>(handlers, teams.toArray(new GameTeam[0]));
		selectors.applyTo(events);

		events.listen(GamePlayerEvents.SPAWN, (playerId, spawn, role) ->
				spawn.run(this::onPlayerWaiting)
		);
	}

	private void onPlayerWaiting(ServerPlayer player) {
		if (teamState.getPollingTeams().size() > 1) {
			for (Component message : MinigameTexts.TEAMS_INTRO) {
				player.sendSystemMessage(message, false);
			}
			selectors.giveSelectorsTo(player);
		}
	}

	private void onRequestJoinTeam(ServerPlayer player, GameTeam team) {
		teamState.setPlayerPreference(player.getUUID(), team.key());

		Component teamName = team.config().name().copy().withColor(team.config().textColor()).withStyle(ChatFormatting.BOLD);
		player.sendSystemMessage(MinigameTexts.JOINED_TEAM.apply(teamName), false);
	}
}
