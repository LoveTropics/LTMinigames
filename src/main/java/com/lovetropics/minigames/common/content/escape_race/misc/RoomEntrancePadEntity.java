package com.lovetropics.minigames.common.content.escape_race.misc;

import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.content.escape_race.client.EscapeRaceRoomsState;
import com.lovetropics.minigames.common.util.PredictedToggle;
import net.minecraft.SharedConstants;
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
				.define(UNLOCKING_TICKS, PredictedToggle.DISABLED);
	}

	@Override
	public boolean canBeCollidedWith(@Nullable Entity entity) {
		if (level().isClientSide()) {
			return isBlockedClientSide();
		}
		return false;
	}

	private boolean isBlockedClientSide() {
		EscapeRaceRoomsState roomsState = ClientGameStateManager.getOrDefault(EscapeRace.ROOMS_STATE, EscapeRaceRoomsState.EMPTY);
		EscapeRaceRoomsState.Room room = roomsState.byEntrance(this);
		return room != null && room.blocked();
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

			// If players get inside before it is blocked, we should push them out
			if (isBlockedClientSide()) {
				level().getPushableEntities(this, getBoundingBox()).forEach(this::push);
			}
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
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putFloat("width", getWidth());
		output.putFloat("height", getHeight());
		output.putFloat("depth", getDepth());
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

	public void setUnlockingTicks(int unlockingTicks, boolean unlocking) {
		getEntityData().set(UNLOCKING_TICKS, PredictedToggle.of(level().getGameTime(), unlockingTicks, unlocking));
	}

	public float getUnlockProgress(float partialTicks) {
		return Mth.lerp(partialTicks, lastUnlockProgress, unlockProgress);
	}
}
