package org.lovetropics.games.common.core.game.behavior.instances;

import com.lovetropics.lib.BlockBox;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.SpawnBuilder;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.behavior.event.GameTeamEvents;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.core.game.region.RegionQuery;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;
import org.lovetropics.games.common.core.game.state.team.GameTeam;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamState;
import org.lovetropics.games.common.core.game.util.CycledSpawner;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class PositionPlayersBehavior implements IGameBehavior {
	private static final Logger LOGGER = LogUtils.getLogger();

	public static final MapCodec<PositionPlayersBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegionQuery.CODEC.optionalFieldOf("participants", RegionQuery.EMPTY).forGetter(c -> c.participantSpawns),
			RegionQuery.CODEC.optionalFieldOf("spectators", RegionQuery.EMPTY).forGetter(c -> c.spectatorSpawns),
			RegionQuery.CODEC.optionalFieldOf("all", RegionQuery.EMPTY).forGetter(c -> c.allSpawns),
			Codec.unboundedMap(GameTeamKey.CODEC, RegionQuery.CODEC).optionalFieldOf("teams", Map.of()).forGetter(c -> c.teamSpawns),
			Codec.BOOL.optionalFieldOf("split_by_team", true).forGetter(c -> c.splitByTeam),
			Codec.FLOAT.optionalFieldOf("angle", 0.0f).forGetter(c -> c.angle),
			RegionQuery.CODEC.optionalFieldOf("face_region").forGetter(c -> c.faceRegion)
	).apply(i, PositionPlayersBehavior::new));

	private final RegionQuery participantSpawns;
	private final RegionQuery spectatorSpawns;
	private final RegionQuery allSpawns;
	private final Map<GameTeamKey, RegionQuery> teamSpawns;
	private final boolean splitByTeam;
	private final float angle;
	private final Optional<RegionQuery> faceRegion;

	private CycledSpawner participantSpawner = CycledSpawner.EMPTY;
	private CycledSpawner spectatorSpawner = CycledSpawner.EMPTY;
	private CycledSpawner fallbackSpawner = CycledSpawner.EMPTY;
	private Map<GameTeamKey, CycledSpawner> teamSpawners = Map.of();

	private @Nullable BlockBox faceBox;

	public PositionPlayersBehavior(RegionQuery participantSpawns, RegionQuery spectatorSpawns, RegionQuery allSpawns, Map<GameTeamKey, RegionQuery> teamSpawns, boolean splitByTeam, float angle, Optional<RegionQuery> faceRegion) {
		this.participantSpawns = participantSpawns;
		this.spectatorSpawns = spectatorSpawns;
		this.allSpawns = allSpawns;
		this.teamSpawns = teamSpawns;
		this.splitByTeam = splitByTeam;
		this.angle = angle;
		this.faceRegion = faceRegion;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		faceRegion.ifPresent(query -> faceBox = query.one(game));

		participantSpawner = new CycledSpawner(game, participantSpawns);
		spectatorSpawner = new CycledSpawner(game, spectatorSpawns);
		fallbackSpawner = new CycledSpawner(game, allSpawns);
		LOGGER.debug("FOUND {} PARTICIPANT SPAWN REGIONS", participantSpawner.regions().size());

		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		if (splitByTeam && teams != null) {
			if (!teamSpawns.isEmpty()) {
				events.listen(GamePlayerEvents.BEFORE_ADD_PLAYERS, (participants, spectators) -> {
					teamSpawners = teamSpawns.entrySet().stream().collect(Collectors.toMap(
							Map.Entry::getKey,
							entry -> new CycledSpawner(game, entry.getValue())
					));
				});
			} else if (!participantSpawner.regions().isEmpty()) {
				events.listen(GameTeamEvents.TEAMS_ALLOCATED, participantTeams -> {
					teamSpawners = createTeamSpawners(teams, participantSpawner, participantTeams);
				});
			}
		}

		events.listen(GamePlayerEvents.SPAWN, (playerId, spawn, role) -> spawnPlayerAsRole(game, playerId, spawn, role, teams));
	}

	private Map<GameTeamKey, CycledSpawner> createTeamSpawners(TeamState teams, CycledSpawner spawns, Map<PlayerKey, GameTeamKey> participants) {
		Map<GameTeamKey, CycledSpawner> teamSpawners = new HashMap<>();

		int spawnCount = spawns.size();
		int groupSize = Math.max(participants.size() / spawnCount, 1);
		for (GameTeam team : teams) {
			int teamSize = (int) participants.values().stream()
					.filter(team.key()::equals)
					.count();
			int teamGroupCount = Math.max(teamSize / groupSize, 1);
			teamSpawners.put(team.key(), spawns.take(teamGroupCount));
		}

		return teamSpawners;
	}

	private void spawnPlayerAsRole(IGamePhase game, UUID playerId, SpawnBuilder spawn, @Nullable PlayerRole role, @Nullable TeamState teams) {
		BlockBox region = getSpawnRegionFor(playerId, role, teams);
		if (region != null) {
			BlockPos pos = tryFindEmptyPos(game, game.random(), region);
			float angle = this.angle;
			if (faceBox != null) {
				angle = getAngleTo(pos, faceBox);
			}
			spawn.teleportTo(game.level(), pos, angle);
		}
	}

	// TODO: Move to LTLib
	public static float getAngleTo(BlockPos fromPos, BlockBox toBox) {
		int nearestX = Mth.clamp(fromPos.getX(), toBox.min().getX(), toBox.max().getX());
		int nearestZ = Mth.clamp(fromPos.getZ(), toBox.min().getZ(), toBox.max().getZ());
		double deltaX = nearestX - (fromPos.getX() + 0.5);
		double deltaZ = nearestZ - (fromPos.getZ() + 0.5);
		return (float) (Mth.atan2(deltaZ, deltaX) * Mth.RAD_TO_DEG - 90.0f);
	}

	private @Nullable BlockBox getSpawnRegionFor(UUID playerId, PlayerRole role, @Nullable TeamState teams) {
		if (role == PlayerRole.PARTICIPANT) {
			BlockBox region = getParticipantSpawnRegion(playerId, teams);
			if (region != null) {
				return region;
			}
		} else if (role == PlayerRole.SPECTATOR) {
			BlockBox region = spectatorSpawner.next();
			if (region != null) {
				return region;
			}
		}
		return fallbackSpawner.next();
	}

	private @Nullable BlockBox getParticipantSpawnRegion(UUID playerId, @Nullable TeamState teams) {
		GameTeamKey team = teams != null ? teams.getTeamForPlayer(playerId) : null;
		if (team != null) {
			CycledSpawner teamSpawner = teamSpawners.get(team);
			if (teamSpawner != null) {
				return teamSpawner.next();
			}
		}
		return participantSpawner.next();
	}

	public static BlockPos tryFindEmptyPos(IGamePhase game, RandomSource random, BlockBox box) {
		ServerLevel level = game.level();
		for (int i = 0; i < 20; i++) {
			BlockPos pos = box.sample(random);
			if (level.isEmptyBlock(pos)) {
				return pos;
			}
		}
		LOGGER.debug("USING FALLBACK SPAWN POS");
		return box.centerBlock();
	}
}
