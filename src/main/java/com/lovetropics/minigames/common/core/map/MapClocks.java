package com.lovetropics.minigames.common.core.map;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.clock.ServerClockManager;
import org.jspecify.annotations.Nullable;

/// Map levels run their own world clocks (see [MapWorldInfo#getOrCreateClockManager]), so clock updates must only
/// reach the players that are in a level driven by those clocks.
public final class MapClocks {
	private MapClocks() {
	}

	public static void broadcast(PlayerList playerList, ServerClockManager clocks, Packet<?> packet) {
		for (ServerPlayer player : playerList.getPlayers()) {
			if (player.level().clockManager() == clocks) {
				player.connection.send(packet);
			}
		}
	}

	public interface Access {
		void ltminigames$setLevel(ServerLevel level);

		@Nullable
		ServerLevel ltminigames$getLevel();
	}
}
