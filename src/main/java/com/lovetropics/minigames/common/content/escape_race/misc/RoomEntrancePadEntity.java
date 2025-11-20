package com.lovetropics.minigames.common.content.escape_race.misc;

import com.lovetropics.minigames.common.util.PredictedToggle;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class RoomEntrancePadEntity extends Entity {

	private static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(RoomEntrancePadEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.defineId(RoomEntrancePadEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DEPTH = SynchedEntityData.defineId(RoomEntrancePadEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Component> NAME = SynchedEntityData.defineId(RoomEntrancePadEntity.class, EntityDataSerializers.COMPONENT);
	private static final EntityDataAccessor<Integer> COST = SynchedEntityData.defineId(RoomEntrancePadEntity.class, EntityDataSerializers.INT);

	public static final int TOTAL_UNLOCK_TICKS = 5 * SharedConstants.TICKS_PER_SECOND;

	private static final EntityDataAccessor<PredictedToggle> UNLOCKING_TICKS = SynchedEntityData.defineId(RoomEntrancePadEntity.class, PredictedToggle.SERIALIZER.get());

	private float lastUnlockProgress;
	private float unlockProgress;

	public RoomEntrancePadEntity(EntityType<?> entityType, Level level) {
		super(entityType, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(WIDTH, 1f)
				.define(HEIGHT, 1f)
				.define(DEPTH, 1f)
				.define(UNLOCKING_TICKS, PredictedToggle.DISABLED)
				.define(NAME, CommonComponents.EMPTY)
				.define(COST, 0);
	}

	@Override
	public boolean canBeCollidedWith(@Nullable Entity entity) {
		return false;
	}

	@Override
	public boolean canCollideWith(Entity entity) {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) {
			lastUnlockProgress = unlockProgress;
			unlockProgress = (float) getEntityData().get(UNLOCKING_TICKS).getCurrentTicks(level().getGameTime(), TOTAL_UNLOCK_TICKS) / TOTAL_UNLOCK_TICKS;
		}
	}

	@Override
	protected AABB makeBoundingBox(Vec3 position) {
		return AABB.ofSize(position, getWidth(), getHeight(), getDepth()).move(0.0, getHeight() / 2.0, 0.0);
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		return EntityDimensions.scalable(getWidth(), getHeight()).withEyeHeight(getHeight());
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		getEntityData().set(WIDTH, input.getFloatOr("width", 1));
		getEntityData().set(HEIGHT, input.getFloatOr("height", 1));
		getEntityData().set(DEPTH, input.getFloatOr("depth", 1));
		getEntityData().set(NAME, input.read("room_name", ComponentSerialization.CODEC).orElse(CommonComponents.EMPTY));
		getEntityData().set(COST, input.getIntOr("cost", 0));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putFloat("width", getWidth());
		output.putFloat("height", getHeight());
		output.putFloat("depth", getDepth());
		output.store("room_name", ComponentSerialization.CODEC, getRoomName());
		output.putInt("cost", getCost());
	}

	public float getWidth() {
		return getEntityData().get(WIDTH);
	}

	public float getDepth() {
		return getEntityData().get(DEPTH);
	}

	public float getHeight() {
		return getEntityData().get(HEIGHT);
	}

	public void setWidth(float width) {
		getEntityData().set(WIDTH, width);
	}

	public void setHeight(float height) {
		getEntityData().set(HEIGHT, height);
	}

	public void setDepth(float depth) {
		getEntityData().set(DEPTH, depth);
	}

	public void setRoomName(Component name) {
		getEntityData().set(NAME, name);
	}

	public Component getRoomName() {
		return getEntityData().get(NAME);
	}

	public void setCost(int cost) {
		getEntityData().set(COST, cost);
	}

	public int getCost() {
		return getEntityData().get(COST);
	}

	public void setUnlockingTicks(int unlockingTicks, boolean unlocking) {
		getEntityData().set(UNLOCKING_TICKS, PredictedToggle.of(level().getGameTime(), unlockingTicks, unlocking));
	}

	public float getUnlockProgress(float partialTicks) {
		return Mth.lerp(partialTicks, lastUnlockProgress, unlockProgress);
	}
}
