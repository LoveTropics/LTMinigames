package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.SpawnBuilder;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GameTeamEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.lovetropics.minigames.common.core.game.util.CycledSpawner;
import com.lovetropics.minigames.common.core.map.MapRegions;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class PositionPlayersBehavior implements IGameBehavior {
	private static final Logger LOGGER = LogUtils.getLogger();

	public static final MapCodec<PositionPlayersBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			MoreCodecs.arrayOrUnit(Codec.STRING, String[]::new).optionalFieldOf("participants", new String[0]).forGetter(c -> c.participantSpawnKeys),
			MoreCodecs.arrayOrUnit(Codec.STRING, String[]::new).optionalFieldOf("spectators", new String[0]).forGetter(c -> c.spectatorSpawnKeys),
			MoreCodecs.arrayOrUnit(Codec.STRING, String[]::new).optionalFieldOf("all", new String[0]).forGetter(c -> c.allSpawnKeys),
			Codec.unboundedMap(GameTeamKey.CODEC, MoreCodecs.arrayOrUnit(Codec.STRING, String[]::new)).optionalFieldOf("teams", Map.of()).forGetter(c -> c.teamSpawnKeys),
			Codec.BOOL.optionalFieldOf("split_by_team", true).forGetter(c -> c.splitByTeam),
			Codec.FLOAT.optionalFieldOf("angle", 0.0f).forGetter(c -> c.angle),
			Codec.STRING.optionalFieldOf("face_region").forGetter(c -> c.faceRegion)
	).apply(i, PositionPlayersBehavior::new));

	private final String[] participantSpawnKeys;
	private final String[] spectatorSpawnKeys;
	private final String[] allSpawnKeys;
	private final Map<GameTeamKey, String[]> teamSpawnKeys;
	private final boolean splitByTeam;
	private final float angle;
	private final Optional<String> faceRegion;

	private CycledSpawner participantSpawner = CycledSpawner.EMPTY;
	private CycledSpawner spectatorSpawner = CycledSpawner.EMPTY;
	private CycledSpawner fallbackSpawner = CycledSpawner.EMPTY;
	private Map<GameTeamKey, CycledSpawner> teamSpawners = Map.of();

	@Nullable
	private BlockBox faceBox;

	public PositionPlayersBehavior(String[] participantSpawnKeys, String[] spectatorSpawnKeys, String[] allSpawnKeys, Map<GameTeamKey, String[]> teamSpawnKeys, boolean splitByTeam, float angle, Optional<String> faceRegion) {
		this.participantSpawnKeys = participantSpawnKeys;
		this.spectatorSpawnKeys = spectatorSpawnKeys;
		this.allSpawnKeys = allSpawnKeys;
		this.teamSpawnKeys = teamSpawnKeys;
		this.splitByTeam = splitByTeam;
		this.angle = angle;
		this.faceRegion = faceRegion;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		MapRegions regions = game.mapRegions();

		faceRegion.ifPresent(key -> faceBox = regions.getOrThrow(key));

		participantSpawner = new CycledSpawner(regions, participantSpawnKeys);
		spectatorSpawner = new CycledSpawner(regions, spectatorSpawnKeys);
		fallbackSpawner = new CycledSpawner(regions, allSpawnKeys);
		LOGGER.debug("FOUND {} PARTICIPANT SPAWN REGIONS", participantSpawner.regions().size());

		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		if (splitByTeam && teams != null) {
			if (!teamSpawnKeys.isEmpty()) {
				events.listen(GamePlayerEvents.BEFORE_ADD_PLAYERS, (participants, spectators) -> {
					teamSpawners = teamSpawnKeys.entrySet().stream().collect(Collectors.toMap(
							Map.Entry::getKey,
							entry -> new CycledSpawner(regions, entry.getValue())
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
			BlockPos pos = tryFindEmptyPos(game, game.level().getRandom(), region);
			float angle = this.angle;
			if (faceBox != null) {
				int nearestX = Mth.clamp(pos.getX(), faceBox.min().getX(), faceBox.max().getX());
				int nearestZ = Mth.clamp(pos.getZ(), faceBox.min().getZ(), faceBox.max().getZ());
				double deltaX = nearestX - (pos.getX() + 0.5);
				double deltaZ = nearestZ - (pos.getZ() + 0.5);
				angle = (float) (Mth.atan2(deltaZ, deltaX) * Mth.RAD_TO_DEG - 90.0f);
			}
			spawn.teleportTo(game.level(), pos, angle);
		}
	}

	@Nullable
	private BlockBox getSpawnRegionFor(UUID playerId, PlayerRole role, @Nullable TeamState teams) {
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

	@Nullable
	private BlockBox getParticipantSpawnRegion(UUID playerId, @Nullable TeamState teams) {
		GameTeamKey team = teams != null ? teams.getTeamForPlayer(playerId) : null;
		if (team != null) {
			CycledSpawner teamSpawner = teamSpawners.get(team);
			if (teamSpawner != null) {
				return teamSpawner.next();
			}
		}
		return participantSpawner.next();
	}

	private BlockPos tryFindEmptyPos(IGamePhase game, RandomSource random, BlockBox box) {
		ServerLevel world = game.level();
		for (int i = 0; i < 20; i++) {
			BlockPos pos = box.sample(random);
			if (world.isEmptyBlock(pos)) {
				return pos;
			}
		}
		LOGGER.debug("USING FALLBACK SPAWN POS");
		return box.centerBlock();
	}
}
