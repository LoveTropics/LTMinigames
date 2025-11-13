package com.lovetropics.minigames.common.content.river_race.behaviour;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.content.river_race.RiverRace;
import com.lovetropics.minigames.common.content.river_race.client_state.RiverRaceClientBarState;
import com.lovetropics.minigames.common.content.river_race.event.RiverRaceEvents;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public record ProgressBehaviour(
		GameTeamKey topTeam,
		GameTeamKey bottomTeam,
		String mapFittingRegion,
		List<LockedZone> lockedZones
) implements IGameBehavior {
	public static final MapCodec<ProgressBehaviour> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			GameTeamKey.CODEC.fieldOf("top_team").forGetter(ProgressBehaviour::topTeam),
			GameTeamKey.CODEC.fieldOf("bottom_team").forGetter(ProgressBehaviour::bottomTeam),
			Codec.STRING.fieldOf("map_fitting_region").forGetter(ProgressBehaviour::mapFittingRegion),
			LockedZone.CODEC.listOf().optionalFieldOf("locked_zones", List.of()).forGetter(ProgressBehaviour::lockedZones)
	).apply(i, ProgressBehaviour::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		MapSpace mapSpace = MapSpace.from(game.mapRegions().getOrThrow(mapFittingRegion));
		TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);

		Progress progress = new Progress(
				Objects.requireNonNull(teams.getTeamByKey(topTeam)),
				Objects.requireNonNull(teams.getTeamByKey(bottomTeam))
		);

		for (LockedZone lockedZone : lockedZones) {
			BlockBox region = game.mapRegions().getOrThrow(lockedZone.id);
			int start = mapSpace.getPos(region.min());
			int end = mapSpace.getPos(region.max());
			progress.lockedZones.put(lockedZone.id, new RiverRaceClientBarState.Zone(start, end - start, lockedZone.color));
		}

		events.listen(GamePhaseEvents.TICK, () -> progress.update(game, mapSpace, teams));
		events.listen(RiverRaceEvents.UNLOCK_ZONE, progress::unlockZone);

		GameClientState.applyGlobally(game, events, SharedConstants.TICKS_PER_SECOND, RiverRace.BAR_STATE.get(), player -> {
			GameTeamKey team = teams.getTeamForPlayer(player);
			return progress.build(team);
		});
	}

	private static class Progress {
		private final TeamProgress topTeam;
		private final TeamProgress bottomTeam;

		private final Map<String, RiverRaceClientBarState.Zone> lockedZones = new HashMap<>();

		private Progress(GameTeam topTeam, GameTeam bottomTeam) {
			this.topTeam = new TeamProgress(topTeam);
			this.bottomTeam = new TeamProgress(bottomTeam);
		}

		public void unlockZone(String id) {
			lockedZones.remove(id);
		}

		public void update(IGamePhase game, MapSpace mapSpace, TeamState teams) {
			topTeam.update(game, mapSpace, teams);
			bottomTeam.update(game, mapSpace, teams);
		}

		// Not using per-team localisation, but keeping as we might want to use it
		public RiverRaceClientBarState build(@Nullable GameTeamKey forTeam) {
			RiverRaceClientBarState.Team topState = topTeam.build();
			RiverRaceClientBarState.Team bottomState = bottomTeam.build();
			return new RiverRaceClientBarState(
					topState, bottomState,
					List.copyOf(lockedZones.values())
			);
		}
	}

	private static class TeamProgress {
		private final GameTeam team;
		private final IntList playerPositions = new IntArrayList();
		private int progress;

		private TeamProgress(GameTeam team) {
			this.team = team;
		}

		public void update(IGamePhase game, MapSpace mapSpace, TeamState teams) {
			playerPositions.clear();
			for (ServerPlayer player : teams.getPlayersForTeam(game, team.key())) {
				int playerPos = mapSpace.getPos(player.blockPosition());
				playerPositions.add(playerPos);
				progress = Math.max(playerPos, progress);
			}
		}

		public RiverRaceClientBarState.Team build() {
			return new RiverRaceClientBarState.Team(team.config().dye(), progress, new IntArrayList(playerPositions));
		}
	}

	private record MapSpace(int min, int max, Direction.Axis axis) {
		public static MapSpace from(BlockBox box) {
			BlockPos size = box.size();
			Direction.Axis axis = size.getX() > size.getZ() ? Direction.Axis.X : Direction.Axis.Z;
			return new MapSpace(box.min().get(axis), box.max().get(axis), axis);
		}

		public int getPos(BlockPos pos) {
			return Mth.clamp(pos.get(axis), min, max) - min;
		}
	}

	private record LockedZone(String id, DyeColor color) {
		public static final Codec<LockedZone> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("id").forGetter(LockedZone::id),
				DyeColor.CODEC.fieldOf("color").forGetter(LockedZone::color)
		).apply(i, LockedZone::new));
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return RiverRace.RIVER_RACE_PROGRESS_BEHAVIOUR;
	}
}
