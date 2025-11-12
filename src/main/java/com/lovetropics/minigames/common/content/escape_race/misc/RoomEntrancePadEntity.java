package com.lovetropics.minigames.common.content.escape_race.misc;

import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.content.escape_race.rooms.RoomStatus;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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

	private static final EntityDataAccessor<Integer> COST = SynchedEntityData.defineId(RoomEntrancePadEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<RoomStatus> ROOM_STATUS = SynchedEntityData.defineId(RoomEntrancePadEntity.class, EscapeRace.ROOM_STATUS);
	private static final EntityDataAccessor<Component> ROOM_NAME = SynchedEntityData.defineId(RoomEntrancePadEntity.class, EntityDataSerializers.COMPONENT);


	public RoomEntrancePadEntity(EntityType<?> entityType, Level level) {
		super(entityType, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(WIDTH, 1f)
				.define(HEIGHT, 1f)
				.define(DEPTH, 1f)
				.define(COST, 1)
				.define(ROOM_STATUS, RoomStatus.LOCKED)
				.define(ROOM_NAME, Component.empty());
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
	public boolean isPickable() {
		return true;
	}

	@Override
	protected AABB makeBoundingBox(Vec3 position) {
		return AABB.ofSize(position, getWidth(), getHeight(), getDepth());
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
		getEntityData().set(COST, input.getIntOr("cost", 1));
		getEntityData().set(ROOM_NAME, input.read("name", ComponentSerialization.CODEC).orElse(Component.empty()));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putFloat("width", getWidth());
		output.putFloat("height", getHeight());
		output.putFloat("depth", getDepth());
		output.storeNullable("name", ComponentSerialization.CODEC, getRoomName());
		output.putInt("cost", getCost());
	}

	public float getWidth() {
		return getEntityData().get(WIDTH);
	}

	public int getCost(){
		return getEntityData().get(COST);
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
	public void setCost(int cost) {
		getEntityData().set(COST, cost);
	}
	public RoomStatus getRoomStatus() {
		return getEntityData().get(ROOM_STATUS);
	}
	public void setRoomStatus(RoomStatus roomStatus) {
		getEntityData().set(ROOM_STATUS, roomStatus);
	}
	public Component getRoomName() {
		return getEntityData().get(ROOM_NAME);
	}

	public void setRoomName(Component name) {
		getEntityData().set(ROOM_NAME, name);
	}
}
