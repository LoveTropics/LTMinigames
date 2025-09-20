package com.lovetropics.minigames.common.content.survive_the_tide.behavior;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLivingEntityEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SttPetsBehavior implements IGameBehavior {
	public static final MapCodec<SttPetsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.unboundedMap(BuiltInRegistries.ENTITY_TYPE.byNameCodec(), PetConfig.CODEC).fieldOf("entities").forGetter(c -> c.petTypes)
	).apply(i, SttPetsBehavior::new));

	private static final double FOLLOW_DISTANCE = 5.0;

	private final Map<EntityType<?>, PetConfig> petTypes;

	private final Object2ObjectMap<UUID, List<Pet>> petsByPlayer = new Object2ObjectOpenHashMap<>();

	private IGamePhase game;

	public SttPetsBehavior(Map<EntityType<?>, PetConfig> petTypes) {
		this.petTypes = petTypes;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		this.game = game;

		events.listen(GamePhaseEvents.TICK, this::tick);

		events.listen(GameLivingEntityEvents.SPAWNED, (entity, reason, player) -> {
			if (reason == EntitySpawnReason.SPAWN_ITEM_USE && player != null && entity instanceof PathfinderMob) {
				onCreatureSpawnedFromEgg((PathfinderMob) entity, player);
			}
		});

		events.listen(GameLivingEntityEvents.DEATH, (entity, damageSource) -> {
			if (entity instanceof PathfinderMob mob) {
				onCreatureDeath(mob);
			}
			return TriState.DEFAULT;
		});

		events.listen(GamePlayerEvents.REMOVE, player -> {
			List<Pet> pets = petsByPlayer.remove(player.getUUID());
			if (pets != null) {
				pets.forEach(Pet::remove);
			}
		});
	}

	private void tick() {
		petsByPlayer.values().removeIf(pets -> {
			pets.removeIf(pet -> pet.tick(game));
			return pets.isEmpty();
		});
	}

	private void onCreatureSpawnedFromEgg(PathfinderMob entity, ServerPlayer player) {
		PetConfig config = petTypes.get(entity.getType());
		if (config != null) {
			stopPetFromGettingSidetracked(entity);

			addPet(new Pet(player, entity, config));
		}
	}

	private void stopPetFromGettingSidetracked(PathfinderMob entity) {
		entity.goalSelector.disableControlFlag(Goal.Flag.MOVE);
		entity.targetSelector.disableControlFlag(Goal.Flag.TARGET);
	}

	private void onCreatureDeath(PathfinderMob entity) {
		Pet pet = findPet(entity);
		if (pet != null) {
			removePet(pet);
		}
	}

	@Nullable
	private Pet findPet(PathfinderMob entity) {
		for (List<Pet> pets : petsByPlayer.values()) {
			for (Pet pet : pets) {
				if (pet.entity == entity) {
					return pet;
				}
			}
		}
		return null;
	}

	private void addPet(Pet pet) {
		List<Pet> petsByPlayer = this.petsByPlayer.computeIfAbsent(pet.player.getUUID(), u -> new ReferenceArrayList<>());
		petsByPlayer.add(pet);
	}

	private void removePet(Pet pet) {
		List<Pet> pets = petsByPlayer.get(pet.player.getUUID());
		pets.remove(pet);

		if (pets.isEmpty()) {
			petsByPlayer.remove(pet.player.getUUID());
		}
	}

	record PetConfig(float attackDamage, float speed) {
		public static final Codec<PetConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.FLOAT.fieldOf("attack_damage").forGetter(c -> c.attackDamage),
				Codec.FLOAT.optionalFieldOf("speed", 1.0F).forGetter(c -> c.speed)
		).apply(i, PetConfig::new));
	}

	static final class Pet {
		private static final int ATTACK_COOLDOWN = 10;

		final ServerPlayer player;
		final PathfinderMob entity;
		final PetConfig config;

		private int attackCooldown;

		Pet(ServerPlayer player, PathfinderMob entity, PetConfig config) {
			this.player = player;
			this.entity = entity;
			this.config = config;
		}

		boolean tick(IGamePhase game) {
			if (!player.isAlive() || !entity.isAlive() || !game.participants().contains(player)) {
				return true;
			}

			tickTargetSelection();

			LivingEntity target = entity.getTarget();
			if (target != null) {
				tickAttacking(game.level(), target);
			} else {
				tickFollowingPlayer();
			}

			return false;
		}

		private void tickTargetSelection() {
			LivingEntity target = player.getLastHurtByMob();
			if (target != null && !target.isAlive()) {
				target = null;
			}

			if (entity.getTarget() != target) {
				entity.setTarget(target);
			}
		}

		private void tickAttacking(ServerLevel serverLevel, LivingEntity target) {
			if (attackCooldown > 0) {
				attackCooldown--;
			}

			double attackDistance = (entity.getBbWidth() + target.getBbWidth()) / 2.0 + 0.5;
			if (entity.distanceToSqr(target) <= attackDistance * attackDistance) {
				tickAttackTarget(serverLevel, target);
			} else {
				tickMoveToTarget(target);
			}
		}

		private void tickAttackTarget(ServerLevel serverLevel, LivingEntity target) {
			if (attackCooldown > 0) {
				return;
			}

			attackCooldown = ATTACK_COOLDOWN;

			DamageSource source = target.damageSources().mobAttack(entity);
			if (target.hurtServer(serverLevel, source, config.attackDamage)) {
				entity.setLastHurtMob(target);
			}
		}

		private void tickMoveToTarget(LivingEntity target) {
			if (!isMovingTowards(target, 2.0)) {
				entity.getNavigation().moveTo(target, config.speed);
			}
		}

		private void tickFollowingPlayer() {
			if (isMovingTowards(player, 3.0)) {
				return;
			}

			double distance2 = entity.distanceToSqr(player);
			if (distance2 > FOLLOW_DISTANCE * FOLLOW_DISTANCE) {
				PathNavigation navigator = entity.getNavigation();
				Path path = navigator.createPath(player, 3);
				if (path != null) {
					navigator.moveTo(path, config.speed);
				}
			}
		}

		private boolean isMovingTowards(LivingEntity target, double threshold) {
			Path path = entity.getNavigation().getPath();
			return path != null && path.getTarget().closerToCenterThan(target.position(), threshold);
		}

		void remove() {
			entity.hurt(entity.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
		}
	}
}
