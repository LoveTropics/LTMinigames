package org.lovetropics.games.common.content.turtle_race;

import com.lovetropics.lib.codec.MoreCodecs;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.util.EntityTemplate;
import org.slf4j.Logger;

import java.util.List;
import java.util.function.Supplier;

@EventBusSubscriber(modid = LoveTropics.ID)
public record RiderBehavior(
		EntityTemplate entity,
		boolean force,
		List<MobEffectInstance> notRidingEffects
) implements IGameBehavior {
	private static final Logger LOGGER = LogUtils.getLogger();

	public static final MapCodec<RiderBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			EntityTemplate.CODEC.fieldOf("entity").forGetter(RiderBehavior::entity),
			Codec.BOOL.optionalFieldOf("force", true).forGetter(RiderBehavior::force),
			MoreCodecs.EFFECT_INSTANCE.listOf().optionalFieldOf("not_riding_effects", List.of()).forGetter(RiderBehavior::notRidingEffects)
	).apply(i, RiderBehavior::new));

	public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, LoveTropics.ID);
	public static final Supplier<AttachmentType<Boolean>> FORCE_RIDER = ATTACHMENT_TYPES.register("force_rider", () -> AttachmentType.builder(() -> true).sync(ByteBufCodecs.BOOL).build());

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GamePlayerEvents.SPAWN, (playerId, spawn, role) -> {
			if (role != PlayerRole.PARTICIPANT) {
				return;
			}
			spawn.run(player -> {
				Entity entity = spawnEntity(player);
				if (entity == null) {
					LOGGER.error("Failed to spawn entity of type: {}", this.entity.type());
					return;
				}
				player.startRiding(entity);
				player.setData(FORCE_RIDER, force);
			});
		});

		events.listen(GamePlayerEvents.SET_ROLE, (player, role, lastRole) -> {
			if (lastRole == PlayerRole.PARTICIPANT) {
				removeVehicle(player);
			}
		});

		events.listen(GamePlayerEvents.TICK, player -> {
			if (player.getVehicle() == null) {
				for (MobEffectInstance effect : notRidingEffects) {
					player.addEffect(new MobEffectInstance(effect));
				}
			}
		});

		events.listen(GamePlayerEvents.REMOVE, RiderBehavior::removeVehicle);
	}

	private static void removeVehicle(ServerPlayer player) {
		Entity vehicle = player.getVehicle();

		player.stopRiding();
		player.removeData(FORCE_RIDER);

		if (vehicle != null) {
			vehicle.kill(player.level());
		}
	}

	@SubscribeEvent
	public static void onDismount(EntityMountEvent event) {
		// Rejecting the dismount of a removed entity is definitely wrong
		if (event.getEntityBeingMounted().isRemoved()) {
			return;
		}
		if (event.isDismounting() && event.getEntityMounting().getExistingData(FORCE_RIDER).orElse(false)) {
			event.setCanceled(true);
		}
	}

	private @Nullable Entity spawnEntity(ServerPlayer player) {
		return entity.spawn(player.level(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
	}
}
