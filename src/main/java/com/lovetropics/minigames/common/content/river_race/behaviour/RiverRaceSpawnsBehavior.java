package com.lovetropics.minigames.common.content.river_race.behaviour;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.content.river_race.RiverRaceState;
import com.lovetropics.minigames.common.content.river_race.event.RiverRaceEvents;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.SpawnBuilder;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.stream.Collectors;

public record RiverRaceSpawnsBehavior(
		Map<String, ZoneSpawns> zoneSpawns
) implements IGameBehavior {
	public static final MapCodec<RiverRaceSpawnsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.unboundedMap(Codec.STRING, ZoneSpawns.CODEC).fieldOf("zone_spawns").forGetter(RiverRaceSpawnsBehavior::zoneSpawns)
	).apply(i, RiverRaceSpawnsBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);
		RiverRaceState riverRaceState = game.state().get(RiverRaceState.KEY);

		Map<String, Map<GameTeamKey, Spawn>> resolvedSpawns = zoneSpawns.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> resolveSpawns(game, entry.getValue())));

		events.listen(GamePlayerEvents.SPAWN, (playerId, spawn, role) -> {
			RiverRaceState.Zone currentZone = riverRaceState.currentZone();
			GameTeamKey team = teams.getTeamForPlayer(playerId);
			if (team == null) {
				return;
			}
			Spawn teamSpawn = resolvedSpawns.getOrDefault(currentZone.id(), Map.of()).get(team);
			if (teamSpawn != null) {
				BlockPos pos = teamSpawn.box.sample(game.random());
				spawn.teleportTo(game.level(), pos, teamSpawn.angle());
				spawn.run(player -> {
					player.connection.send(new ClientboundClearTitlesPacket(true));
					player.connection.send(new ClientboundSetTitlesAnimationPacket(SharedConstants.TICKS_PER_SECOND / 2, 2 * SharedConstants.TICKS_PER_SECOND, SharedConstants.TICKS_PER_SECOND / 2));
					player.connection.send(new ClientboundSetTitleTextPacket(currentZone.ordinalName()));
					player.connection.send(new ClientboundSetSubtitleTextPacket(currentZone.displayName()));
				});
			}
		});

		events.listen(RiverRaceEvents.MICROGAMES_ENDED, () -> {
			// Bring all players to the new zone
			for (ServerPlayer player : game.participants()) {
				SpawnBuilder spawn = new SpawnBuilder(player);
				game.invoker(GamePlayerEvents.SPAWN).onSpawn(player.getUUID(), spawn, PlayerRole.PARTICIPANT);
				spawn.teleportAndApply(player);
			}
		});
	}

	private static Map<GameTeamKey, Spawn> resolveSpawns(IGamePhase game, ZoneSpawns spawns) {
		return spawns.regionByTeam().entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, regionEntry -> {
					BlockBox box = game.mapRegions().getOrThrow(regionEntry.getValue());
					return new Spawn(box, spawns.angle);
				}));
	}

	private record Spawn(BlockBox box, float angle) {
	}

	public record ZoneSpawns(Map<GameTeamKey, String> regionByTeam, float angle) {
		public static final Codec<ZoneSpawns> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.unboundedMap(GameTeamKey.CODEC, Codec.STRING).fieldOf("region_by_team").forGetter(ZoneSpawns::regionByTeam),
				Codec.FLOAT.optionalFieldOf("angle", 0.0f).forGetter(ZoneSpawns::angle)
		).apply(i, ZoneSpawns::new));
	}
}
