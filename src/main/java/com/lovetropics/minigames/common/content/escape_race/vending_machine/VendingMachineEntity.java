package com.lovetropics.minigames.common.content.escape_race.vending_machine;

import net.minecraft.core.Direction;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class VendingMachineEntity extends Entity {
	public VendingMachineEntity(EntityType<? extends Entity> entityType, Level level) {
		super(entityType, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {

	}

	@Override
	public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float v) {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput valueInput) {

	}

	@Override
	protected void addAdditionalSaveData(ValueOutput valueOutput) {

	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean canBeCollidedWith(@Nullable Entity entity) {
		return true;
	}

	@Override
	public boolean canCollideWith(Entity entity) {
		return (entity.canBeCollidedWith(this) || entity.isPushable()) && !isPassengerOfSameVehicle(entity);
	}

	@Override
	protected AABB makeBoundingBox(Vec3 position) {
		var facing = this.getDirection();
		Vec3 vec31 = position.relative(facing, 0.5f);
		Vec3 vec32 = position.relative(facing.getOpposite(), 0.5f);
		for (Direction.Axis value : Direction.Axis.values()) {
			if(value == facing.getAxis() || value == Direction.Axis.Y){
				continue;
			}
			vec31 = vec31.relative(value.getPositive(), 1f);
			vec32 = vec32.relative(value.getNegative(), 1f);
		}
		return new AABB(vec31.x, position.y, vec31.z, vec32.x, position.y + 3f, vec32.z);
	}
}
