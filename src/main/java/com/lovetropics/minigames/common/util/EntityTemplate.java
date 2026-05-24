package com.lovetropics.minigames.common.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record EntityTemplate(EntityType<?> type, CompoundTag tag) {
	public static final MapCodec<EntityTemplate> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("type").forGetter(EntityTemplate::type),
			CompoundTag.CODEC.optionalFieldOf("tag", new CompoundTag()).forGetter(EntityTemplate::tag)
	).apply(i, EntityTemplate::new));

	public static final Codec<EntityTemplate> CODEC = Codec.withAlternative(
			MAP_CODEC.codec(),
			BuiltInRegistries.ENTITY_TYPE.byNameCodec(),
			EntityTemplate::new
	);

	public EntityTemplate(EntityType<?> type) {
		this(type, new CompoundTag());
	}

	@Nullable
	public Entity create(ServerLevel level, double x, double y, double z, float yRot, float xRot) {
		CompoundTag tag = this.tag.copy();
		tag.putString("id", EntityType.getKey(type).toString());
		return EntityType.loadEntityRecursive(tag, level, EntitySpawnReason.COMMAND, e -> {
			e.snapTo(x, y, z, yRot, xRot);
			e.setYRot(yRot);
			e.setYBodyRot(yRot);
			e.setYHeadRot(yRot);
			return e;
		});
	}

	@Nullable
	public Entity spawn(ServerLevel level, double x, double y, double z) {
		return spawn(level, x, y, z, level.getRandom().nextFloat() * 360.0f, 0.0f);
	}

	@Nullable
	public Entity spawn(ServerLevel level, double x, double y, double z, float yRot, float xRot) {
		return spawn(level, x, y, z, yRot, xRot, EntitySpawnReason.COMMAND);
	}

	@Nullable
	public Entity spawn(ServerLevel level, double x, double y, double z, float yRot, float xRot, EntitySpawnReason spawnType) {
		if (type == EntityType.LIGHTNING_BOLT) {
			return spawnLightningBolt(level, x, y, z);
		}

		Entity entity = create(level, x, y, z, yRot, xRot);
		if (entity != null) {
			if (entity instanceof Mob mob) {
				mob.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), spawnType, null);
			}
			return level.tryAddFreshEntityWithPassengers(entity) ? entity : null;
		}

		return null;
	}

	@Nullable
	private static LightningBolt spawnLightningBolt(ServerLevel level, double x, double y, double z) {
		LightningBolt entity = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.COMMAND);
		if (entity != null) {
			entity.snapTo(new Vec3(x, y, z));
			level.addFreshEntity(entity);
			return entity;
		}
		return null;
	}
}
