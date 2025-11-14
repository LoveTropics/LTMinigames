package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior;

import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.tutorial.TutorialState;
import com.lovetropics.minigames.common.content.biodiversity_blitz.client_state.CheckeredPlotsState;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.PlotsState;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.state.GameStateMap;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.lovetropics.minigames.common.core.map.MapRegions;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.Map;

public final class BbAssignPlotsBehavior implements IGameBehavior {
	public static final MapCodec<BbAssignPlotsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Plot.RegionKeys.CODEC.fieldOf("regions").forGetter(c -> c.regionKeys),
			Codec.unboundedMap(GameTeamKey.CODEC, Plot.Config.CODEC).fieldOf("plots").forGetter(c -> c.teamPlots)
	).apply(i, BbAssignPlotsBehavior::new));

	private static final Logger LOGGER = LogUtils.getLogger();

	private final Plot.RegionKeys regionKeys;
	private final Map<GameTeamKey, Plot.Config> teamPlots;

	private TeamState teams;
	private PlotsState plots;

	public BbAssignPlotsBehavior(Plot.RegionKeys regionKeys, Map<GameTeamKey, Plot.Config> teamPlots) {
		this.regionKeys = regionKeys;
		this.teamPlots = teamPlots;
	}

	@Override
	public void registerState(IGamePhase game, GameStateMap phaseState, GameStateMap instanceState) {
		teams = instanceState.getOrThrow(TeamState.KEY);
		plots = phaseState.register(PlotsState.KEY, new PlotsState(teams));
		// TODO: find a better place for this
		phaseState.register(TutorialState.KEY, new TutorialState());
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		MapRegions regions = game.mapRegions();

		events.listen(GamePhaseEvents.CREATE, () -> {
			teamPlots.forEach((teamKey, config) -> {
				if (teams.getTeamByKey(teamKey) == null) {
					throw new GameException(Component.literal("Game team does not exist: " + teamKey));
				}
				Plot plot = Plot.create(game.level(), teamKey, config, regionKeys, regions);
				plots.addTeamPlot(teamKey, plot);
				game.invoker(BbEvents.CREATE_PLOT).onCreatePlot(plot);
			});

			applyCheckeredPlots(events);
		});

		events.listen(GamePlayerEvents.SET_ROLE, (player, role, lastRole) -> {
			if (role == PlayerRole.PARTICIPANT) {
				trySpawnParticipant(game, player);
			}
		});
	}

	private void applyCheckeredPlots(EventRegistrar events) {
		CheckeredPlotsState checkeredPlots = new CheckeredPlotsState(
				plots.stream().map(plot -> plot.floorBounds).toList()
		);

		GameClientState.applyGlobally(checkeredPlots, events);
	}

	private void trySpawnParticipant(IGamePhase game, ServerPlayer player) {
		GameTeamKey team = teams.getTeamForPlayer(player);
		if (team == null) {
			LOGGER.warn("{} joined as participant but without a team, setting as spectator", player.getGameProfile().getName());
			game.setPlayerRole(player, PlayerRole.SPECTATOR);
			return;
		}

		Plot plot = plots.getPlotFor(team);
		game.invoker(BbEvents.ASSIGN_PLOT).onAssignPlot(player, plot);
	}
}
