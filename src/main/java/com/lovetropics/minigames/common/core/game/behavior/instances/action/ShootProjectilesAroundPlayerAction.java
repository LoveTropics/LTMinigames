package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

import java.util.Iterator;
import java.util.UUID;

public class ShootProjectilesAroundPlayerAction implements IGameBehavior {
	public static final MapCodec<ShootProjectilesAroundPlayerAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.optionalFieldOf("entity_count_per_player", 10).forGetter(c -> c.entityCountPerPlayer),
			Codec.INT.optionalFieldOf("spawn_distance_max", 40).forGetter(c -> c.spawnDistanceMax),
			Codec.INT.optionalFieldOf("target_randomness", 10).forGetter(c -> c.targetRandomness),
			Codec.INT.optionalFieldOf("spawn_height", 20).forGetter(c -> c.spawnRangeY),
			Codec.INT.optionalFieldOf("spawn_rate_base", 20).forGetter(c -> c.spawnRateBase),
			Codec.INT.optionalFieldOf("spawn_rate_random", 20).forGetter(c -> c.spawnRateRandom),
			Codec.INT.optionalFieldOf("explosion_strength", 2).forGetter(c -> c.explosionStrength)
	).apply(i, ShootProjectilesAroundPlayerAction::new));

	//private final Identifier entityId;
	private final int entityCountPerPlayer;
	private final int spawnDistanceMax;
	private final int targetRandomness;
	private final int spawnRangeY;
	private final int spawnRateBase;
	private final int spawnRateRandom;
	private final int explosionStrength;
	private final Object2IntMap<UUID> playerToAmountToSpawn = new Object2IntOpenHashMap<>();
	private final Object2IntMap<UUID> playerToDelayToSpawn = new Object2IntOpenHashMap<>();

	public ShootProjectilesAroundPlayerAction(/*final Identifier entityId, */final int entityCount, final int spawnDistanceMax, final int spawnRangeY, final int spawnsPerTickBase, final int spawnsPerTickRandom, final int targetRandomness, final int explosionStrength) {
		//this.entityId = entityId;
		entityCountPerPlayer = entityCount;
		this.spawnDistanceMax = spawnDistanceMax;
		this.targetRandomness = targetRandomness;
		this.spawnRangeY = spawnRangeY;
		spawnRateBase = spawnsPerTickBase;
		spawnRateRandom = spawnsPerTickRandom;
		this.explosionStrength = explosionStrength;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.applyToPlayers(game, (context, player) -> {
			playerToAmountToSpawn.put(player.getUUID(), entityCountPerPlayer);
			return true;
		});
		events.listen(GamePhaseEvents.TICK, () -> tick(game));
	}

	private void tick(IGamePhase game) {
		Iterator<Object2IntMap.Entry<UUID>> it = playerToAmountToSpawn.object2IntEntrySet().iterator();
		while (it.hasNext()) {
			Object2IntMap.Entry<UUID> entry = it.next();

			UUID id = entry.getKey();
			ServerPlayer player = game.participants().getPlayerBy(id);
			if (player == null || !player.isAlive()) {
				it.remove();
				playerToDelayToSpawn.removeInt(id);
				continue;
			}

			int cooldown = playerToDelayToSpawn.getOrDefault(id, 0);
			if (cooldown > 0) {
				cooldown--;
				playerToDelayToSpawn.put(id, cooldown);
			} else {
				ServerLevel world = game.level();
				RandomSource random = world.getRandom();

				cooldown = spawnRateBase + random.nextInt(spawnRateRandom);
				playerToDelayToSpawn.put(id, cooldown);

				BlockPos posSpawn = player.blockPosition().offset(random.nextInt(spawnDistanceMax * 2) - spawnDistanceMax, 20,
						random.nextInt(spawnDistanceMax * 2) - spawnDistanceMax);

				BlockPos posTarget = player.blockPosition().offset(random.nextInt(targetRandomness * 2) - targetRandomness, 0,
						random.nextInt(targetRandomness * 2) - targetRandomness);

				int newAmount = entry.getIntValue() - 1;
				if (newAmount <= 0) {
					playerToDelayToSpawn.removeInt(id);
					it.remove();
				} else {
					entry.setValue(newAmount);
				}
				LargeFireball fireball = createFireball(world, posSpawn, posTarget);
				world.addFreshEntity(fireball);
			}
		}
	}

	private LargeFireball createFireball(ServerLevel world, BlockPos spawn, BlockPos target) {
		LargeFireball fireball = new LargeFireball(EntityTypes.FIREBALL, world) {
			@Override
			protected void onHit(final HitResult hitResult) {
				switch (hitResult.getType()) {
					case ENTITY -> onHitEntity((EntityHitResult) hitResult);
					case BLOCK -> onHitBlock((BlockHitResult) hitResult);
				}

				if (!level().isClientSide()) {
					boolean mobGriefing = EventHooks.canEntityGrief(world, getOwner());
					level().explode(null, getX(), getY(), getZ(), explosionStrength, mobGriefing, Level.ExplosionInteraction.MOB);
					discard();
				}
			}
		};

		fireball.snapTo(spawn.getX(), spawn.getY(), spawn.getZ(), fireball.getYRot(), fireball.getXRot());
		Vec3 direction = Vec3.atCenterOf(target).subtract(Vec3.atCenterOf(spawn));
		fireball.setDeltaMovement(direction.normalize().scale(fireball.accelerationPower));

		return fireball;
	}
}
