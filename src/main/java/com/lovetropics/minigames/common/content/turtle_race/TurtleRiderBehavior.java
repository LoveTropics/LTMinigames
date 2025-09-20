package com.lovetropics.minigames.common.content.turtle_race;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.util.EntityTemplate;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;

public record TurtleRiderBehavior(EntityTemplate turtle) implements IGameBehavior {
	private static final Logger LOGGER = LogUtils.getLogger();

	public static final MapCodec<TurtleRiderBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			EntityTemplate.CODEC.fieldOf("turtle").forGetter(TurtleRiderBehavior::turtle)
	).apply(i, TurtleRiderBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		Map<UUID, Entity> turtles = new Object2ObjectOpenHashMap<>();

		events.listen(GamePlayerEvents.SPAWN, (playerId, spawn, role) -> {
			if (role != PlayerRole.PARTICIPANT) {
				return;
			}
			spawn.run(player -> {
				Entity entity = spawnTurtle(player);
				if (entity == null) {
					LOGGER.error("Failed to spawn turtle entity of type: {}", turtle.type());
					return;
				}
				player.startRiding(entity);
				turtles.put(player.getUUID(), entity);
			});
		});

		events.listen(GamePlayerEvents.SET_ROLE, (player, role, lastRole) -> {
			if (lastRole == PlayerRole.PARTICIPANT) {
				removeTurtle(turtles, player);
			}
		});

		events.listen(GamePlayerEvents.REMOVE, player -> removeTurtle(turtles, player));

		events.listen(GamePlayerEvents.TICK, player -> {
			Entity turtle = turtles.get(player.getUUID());
			if (turtle == null) {
				return;
			}

			if (player.getVehicle() != turtle) {
				fixTurtle(turtles, player, turtle);
			}
		});
	}

	private static void removeTurtle(Map<UUID, Entity> turtles, ServerPlayer player) {
		player.stopRiding();

		Entity turtle = turtles.remove(player.getUUID());
		if (turtle != null) {
			turtle.kill(player.level());
		}
	}

	private void fixTurtle(Map<UUID, Entity> turtles, ServerPlayer player, Entity turtle) {
		if (!turtle.isAlive()) {
			turtle = spawnTurtle(player);
			if (turtle == null) {
				turtles.remove(player.getUUID());
				return;
			}
			turtles.put(player.getUUID(), turtle);
		}

		player.startRiding(turtle, true);

		ServerChunkCache chunkSource = player.level().getChunkSource();
		chunkSource.chunkMap.broadcast(turtle, new ClientboundSetPassengersPacket(turtle));
	}

	@Nullable
	private Entity spawnTurtle(ServerPlayer player) {
		return turtle.spawn(player.level(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
	}
}
