package com.lovetropics.minigames.common.util;

import com.lovetropics.lib.BlockBox;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Util {
	public static final AABB INFINITE_AABB = new AABB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

	public static boolean spawnEntity(EntityType<?> entityType, Level world, double x, double y, double z, EntitySpawnReason reason) {
		if (entityType == EntityType.LIGHTNING_BOLT) {
			LightningBolt entity = EntityType.LIGHTNING_BOLT.create(world, reason);
			entity.snapTo(new Vec3(x, y, z));
			world.addFreshEntity(entity);
			return true;
		} else {
			final Entity entity = entityType.create(world, reason);
			if (entity != null) {
				entity.setPos(x, y, z);
				return world.addFreshEntity(entity);
			}

			return false;
		}
	}

	public static boolean addItemStackToInventory(final ServerPlayer player, final ItemStack itemstack) {
		if (player.addItem(itemstack)) {
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
			return true;
		} else {
			ItemEntity item = player.drop(itemstack, false);
			if (item != null) {
				item.setNoPickUpDelay();
				item.setThrower(player);
			}
		}

		return false;
	}

	@Nullable
	public static BlockPos findGround(Level world, BlockPos origin, int maximumDistance) {
		if (!isSolidGround(world, origin)) {
			BlockPos pos = origin.below();
			if (world.getBlockState(pos).isSolid()) {
				return origin;
			}
		}

		// if this position is not free, scan upwards to find the ground
		if (world.getBlockState(origin).isSolid()) {
			BlockPos.MutableBlockPos mutablePos = origin.mutable();

			for (int i = 0; i < maximumDistance; i++) {
				mutablePos.move(Direction.UP);
				if (world.isOutsideBuildHeight(mutablePos) || !world.getBlockState(mutablePos).isSolid()) {
					return mutablePos.immutable();
				}
			}
		}

		// if the position below us is not solid, scan downwards to find the ground
		BlockPos pos = origin.below();
		if (!world.getBlockState(pos).isSolid()) {
			BlockPos.MutableBlockPos mutablePos = origin.mutable();

			for (int i = 0; i < maximumDistance; i++) {
				mutablePos.move(Direction.DOWN);
				if (world.isOutsideBuildHeight(mutablePos)) {
					return null;
				}

				if (world.getBlockState(mutablePos).isSolid()) {
					return mutablePos.move(Direction.UP).immutable();
				}
			}
		}

		return null;
	}

	private static boolean isSolidGround(Level world, BlockPos pos) {
		return world.getBlockState(pos).isSolid();
	}

	public static void drawParticleBetween(ParticleOptions data, Vec3 start, Vec3 end, ServerLevel world, RandomSource random, int count, double xzScale, double yScale, double speedBase, double speedScale) {
		for (int i = 0; i < count; i++) {
			Vec3 sample = lerpVector(start, end, i / 20.0);
			double d3 = random.nextGaussian() * xzScale;
			double d1 = random.nextGaussian() * yScale;
			double d2 = random.nextGaussian() * xzScale;
			world.sendParticles(data, sample.x, sample.y, sample.z, 1 + random.nextInt(2), d3, d1, d2, speedBase + random.nextDouble() * speedScale);
		}
	}

	public static void spawnDamageParticles(Mob mob, BlockPos pos, int damage) {
		RandomSource random = mob.level().getRandom();
		for (int i = 0; i < 2 + (damage / 2); ++i) {
			double dx = random.nextGaussian() * 0.02;
			double dy = random.nextGaussian() * 0.02;
			double dz = random.nextGaussian() * 0.02;

			((ServerLevel) mob.level()).sendParticles(
					ParticleTypes.ANGRY_VILLAGER,
					pos.getX() + 0.5,
					pos.getY() + 0.5,
					pos.getZ() + 0.5,
					1 + random.nextInt(2),
					dx, dy, dz,
					0.01 + random.nextDouble() * 0.02
			);
		}
	}

	public static Vec3 lerpVector(Vec3 start, Vec3 end, double d) {
		return new Vec3(Mth.lerp(d, start.x, end.x), Mth.lerp(d, start.y, end.y), Mth.lerp(d, start.z, end.z));
	}

	public static Direction getDirectionBetween(BlockBox from, BlockBox to) {
		BlockPos fromCenter = from.centerBlock();
		BlockPos toCenter = to.centerBlock();
		int dx = toCenter.getX() - fromCenter.getX();
		int dz = toCenter.getZ() - fromCenter.getZ();
		if (Math.abs(dx) > Math.abs(dz)) {
			return dx > 0 ? Direction.EAST : Direction.WEST;
		} else {
			return dz > 0 ? Direction.SOUTH : Direction.NORTH;
		}
	}

	public static String formatMinutesSeconds(long totalSeconds) {
		long minutesPart = totalSeconds / 60;
		long secondsPart = totalSeconds % 60;
		return String.format("%02d:%02d", minutesPart, secondsPart);
	}

	public static String unpackTranslationKey(final Component component) {
		return ((TranslatableContents) component.getContents()).getKey();
	}

	@Nullable
	public static ServerPlayer getKillerPlayer(final ServerPlayer player, final DamageSource killingBlow) {
		if (killingBlow.getEntity() instanceof final ServerPlayer killerPlayer) {
			return killerPlayer;
		} else if (player.getKillCredit() instanceof final ServerPlayer killerPlayer) {
			return killerPlayer;
		}
		return null;
	}

	public static CompletableFuture<Optional<GameProfile>> getProfile(MinecraftServer server, UUID uuid) {
		CompletableFuture<Optional<GameProfile>> future = new CompletableFuture<>();
		ProfileResolver resolver = server.services().profileResolver();
		net.minecraft.util.Util.nonCriticalIoPool().execute(() -> future.complete(resolver.fetchById(uuid)));
		return future;
	}

	public static CompletableFuture<Optional<GameProfile>> getProfile(MinecraftServer server, String name) {
		CompletableFuture<Optional<GameProfile>> future = new CompletableFuture<>();
		ProfileResolver resolver = server.services().profileResolver();
		net.minecraft.util.Util.nonCriticalIoPool().execute(() -> future.complete(resolver.fetchByName(name)));
		return future;
	}

	public static void sendNotifySound(ServerPlayer serverPlayer, SoundEvent soundEvent, SoundSource soundSource, float f, float g) {
		serverPlayer.connection.send(new ClientboundSoundPacket(
				BuiltInRegistries.SOUND_EVENT.wrapAsHolder(soundEvent),
				soundSource, serverPlayer.getX(),
				serverPlayer.getY(),
				serverPlayer.getZ(),
				f,
				g,
				serverPlayer.getRandom().nextLong()));
	}
}
