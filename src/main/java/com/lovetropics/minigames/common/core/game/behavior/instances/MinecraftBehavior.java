package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record MinecraftBehavior(

) implements IGameBehavior {
	public static final MapCodec<MinecraftBehavior> CODEC = MapCodec.unit(MinecraftBehavior::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		Set<UUID> playersToSpawnProperly = new HashSet<>();
		events.listen(GamePlayerEvents.SPAWN, (playerId, spawn, role) -> {
			spawn.teleportTo(game.level(), game.level().getSharedSpawnPos());
			if (role == PlayerRole.PARTICIPANT) {
				playersToSpawnProperly.add(playerId);
			}
		});

		// This is a bit of hack because we need the player instance to respawn - until future versions!
		events.listen(GamePlayerEvents.TICK, player -> {
			if (playersToSpawnProperly.remove(player.getUUID())) {
				spawnPlayer(game, player);
			}
		});
	}

	private void spawnPlayer(IGamePhase game, ServerPlayer player) {
		TeleportTransition respawn = player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
		if (respawn.missingRespawnBlock() || respawn.newLevel() == game.server().overworld()) {
			BlockPos pos = player.adjustSpawnLocation(game.level(), game.level().getSharedSpawnPos());
			player.teleportTo(game.level(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Set.of(), 0.0f, 0.0f, true);
		} else {
			player.teleport(respawn);
		}
	}
}
