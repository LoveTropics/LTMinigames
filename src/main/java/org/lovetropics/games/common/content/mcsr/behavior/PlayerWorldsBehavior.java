package org.lovetropics.games.common.content.mcsr.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.map.GameMapProviders;
import org.lovetropics.games.common.core.game.map.IGameMapProvider;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.core.game.region.Region;
import org.lovetropics.games.common.core.game.region.RegionQuery;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/// Gives every participant their own copy of the same generated world, each running as a sub-phase of this game.
/// The world config must use a generator map: its seed is replaced with one shared by every player of this game.
public record PlayerWorldsBehavior(
		Identifier world,
		IGameMapProvider map,
		Optional<Long> seed,
		RegionQuery spawnRegion
) implements IGameBehavior {
	public static final MapCodec<PlayerWorldsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Identifier.CODEC.fieldOf("world").forGetter(PlayerWorldsBehavior::world),
			GameMapProviders.CODEC.fieldOf("map").forGetter(PlayerWorldsBehavior::map),
			Codec.LONG.optionalFieldOf("seed").forGetter(PlayerWorldsBehavior::seed),
			RegionQuery.CODEC.fieldOf("spawn_region").forGetter(PlayerWorldsBehavior::spawnRegion)
	).apply(i, PlayerWorldsBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		Map<UUID, CompletableFuture<ServerLevel>> levelFutures = new HashMap<>();
		events.listen(GamePlayerEvents.BEFORE_ADD_PLAYERS, (participants, _) -> {
			for (PlayerKey participant : participants) {
				levelFutures.put(participant.id(), game.createLevelHackyMightNotWork(map));
			}
		});

		events.listen(GamePlayerEvents.SPAWN, (playerId, spawn, role) -> {
			if (role == PlayerRole.PARTICIPANT) {
				// TODO: Syncing on this sucks, but it's ok for now
				CompletableFuture<ServerLevel> future = levelFutures.get(playerId);
				game.server().managedBlock(future::isDone);
				ServerLevel level = future.join();

				Region spawnRegion = this.spawnRegion.oneIn(game, level);
				spawn.teleportTo(level, spawnRegion.center());
			}
		});
	}
}
