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

public record RiderBehavior(EntityTemplate entity) implements IGameBehavior {
	private static final Logger LOGGER = LogUtils.getLogger();

	public static final MapCodec<RiderBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			EntityTemplate.CODEC.fieldOf("entity").forGetter(RiderBehavior::entity)
	).apply(i, RiderBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		Map<UUID, Entity> riddenEntities = new Object2ObjectOpenHashMap<>();

		events.listen(GamePlayerEvents.SPAWN, (playerId, spawn, role) -> {
			if (role != PlayerRole.PARTICIPANT) {
				return;
			}
			spawn.run(player -> {
				Entity entity = spawnEntity(player);
				if (entity == null) {
					LOGGER.error("Failed to spawn entity of type: {}", entity.getType());
					return;
				}
				player.startRiding(entity);
				riddenEntities.put(player.getUUID(), entity);
			});
		});

		events.listen(GamePlayerEvents.SET_ROLE, (player, role, lastRole) -> {
			if (lastRole == PlayerRole.PARTICIPANT) {
				removeEntity(riddenEntities, player);
			}
		});

		events.listen(GamePlayerEvents.REMOVE, player -> removeEntity(riddenEntities, player));

		events.listen(GamePlayerEvents.TICK, player -> {
			Entity entity = riddenEntities.get(player.getUUID());
			if (entity == null) {
				return;
			}

			if (player.getVehicle() != entity) {
				fixEntity(riddenEntities, player, entity);
			}
		});
	}

	private static void removeEntity(Map<UUID, Entity> riddenEntities, ServerPlayer player) {
		player.stopRiding();

		Entity entity = riddenEntities.remove(player.getUUID());
		if (entity != null) {
			entity.kill(player.level());
		}
	}

	private void fixEntity(Map<UUID, Entity> riddenEntities, ServerPlayer player, Entity entity) {
		if (!entity.isAlive()) {
			entity = spawnEntity(player);
			if (entity == null) {
				riddenEntities.remove(player.getUUID());
				return;
			}
			riddenEntities.put(player.getUUID(), entity);
		}

		player.startRiding(entity, true);

		ServerChunkCache chunkSource = player.level().getChunkSource();
		chunkSource.chunkMap.broadcast(entity, new ClientboundSetPassengersPacket(entity));
	}

	@Nullable
	private Entity spawnEntity(ServerPlayer player) {
		return entity.spawn(player.level(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
	}
}
