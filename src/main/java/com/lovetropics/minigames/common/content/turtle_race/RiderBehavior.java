package com.lovetropics.minigames.common.content.turtle_race;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.util.EntityTemplate;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@EventBusSubscriber
public record RiderBehavior(EntityTemplate entity, boolean force) implements IGameBehavior {
	private static final Logger LOGGER = LogUtils.getLogger();

	public static final MapCodec<RiderBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			EntityTemplate.CODEC.fieldOf("entity").forGetter(RiderBehavior::entity),
			Codec.BOOL.optionalFieldOf("force", true).forGetter(RiderBehavior::force)
	).apply(i, RiderBehavior::new));

	public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, LoveTropics.ID);
	public static final Supplier<AttachmentType<Boolean>> FORCE_RIDER = ATTACHMENT_TYPES.register("force_rider", () -> AttachmentType.builder(() -> true).sync(ByteBufCodecs.BOOL).build());

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
				player.setData(FORCE_RIDER, force);
				riddenEntities.put(player.getUUID(), entity);
			});
		});

		events.listen(GamePlayerEvents.SET_ROLE, (player, role, lastRole) -> {
			if (lastRole == PlayerRole.PARTICIPANT) {
				removeEntity(riddenEntities, player);
			}
		});

		events.listen(GamePlayerEvents.REMOVE, player -> removeEntity(riddenEntities, player));
	}

	private static void removeEntity(Map<UUID, Entity> riddenEntities, ServerPlayer player) {
		player.stopRiding();
		player.removeData(FORCE_RIDER);

		Entity entity = riddenEntities.remove(player.getUUID());
		if (entity != null) {
			entity.kill(player.level());
		}
	}

	@SubscribeEvent
	public static void onDismount(EntityMountEvent event) {
		if (event.isDismounting() && event.getEntityMounting().getExistingData(FORCE_RIDER).orElse(false)) {
			event.setCanceled(true);
		}
	}

	private @Nullable Entity spawnEntity(ServerPlayer player) {
		return entity.spawn(player.level(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
	}
}
