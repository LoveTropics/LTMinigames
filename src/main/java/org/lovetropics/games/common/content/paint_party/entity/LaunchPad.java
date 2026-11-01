package org.lovetropics.games.common.content.paint_party.entity;

import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class LaunchPad extends Entity {
	private static final EntityDataAccessor<Float> DATA_LAUNCH_SCALE = SynchedEntityData.defineId(LaunchPad.class, EntityDataSerializers.FLOAT);

	private static final float DEFAULT_LAUNCH_SCALE = 1.0f;

	private final Set<UUID> pushedLastTick = new ObjectArraySet<>();

	public LaunchPad(EntityType<? extends LaunchPad> entityType, Level level) {
		super(entityType, level);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		setYRot(getYRot() + 3.0f);
		setYBodyRot(getYRot());

		if (level().isClientSide()) {
			level().addParticle(
					ParticleTypes.TRIAL_SPAWNER_DETECTED_PLAYER_OMINOUS,
					true,
					true,
					getX() + random.triangle(0.0f, 0.2f),
					getY() + 0.3,
					getZ() + random.triangle(0.0f, 0.2f),
					0.0,
					0.015,
					0.0
			);
		}

		double size = 1.1;
		List<Entity> pushableEntities = level().getPushableEntities(this, AABB.ofSize(position().add(0.0, 0.1, 0.0), size, size, size));
		if (pushableEntities.isEmpty()) {
			pushedLastTick.clear();
		} else {
			Set<UUID> pushedThisTick = new ObjectArraySet<>();
			for (Entity entity : pushableEntities) {
				if (!pushedLastTick.contains(entity.getUUID())) {
					Vec3 deltaMovement = getDeltaMovement();
					entity.setDeltaMovement(deltaMovement.with(Direction.Axis.Y, getLaunchScale()));
					entity.playSound(SoundEvents.WIND_CHARGE_BURST.value(), 1.0f, 1.0f);
				}
				pushedThisTick.add(entity.getUUID());
			}
			pushedLastTick.clear();
			pushedLastTick.addAll(pushedThisTick);
		}
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_LAUNCH_SCALE, DEFAULT_LAUNCH_SCALE);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		setLaunchScale(input.getFloatOr("launch_scale", DEFAULT_LAUNCH_SCALE));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putFloat("launch_scale", getLaunchScale());
	}

	public void setLaunchScale(float launchScale) {
		entityData.set(DATA_LAUNCH_SCALE, launchScale);
	}

	public float getLaunchScale() {
		return entityData.get(DATA_LAUNCH_SCALE);
	}
}
