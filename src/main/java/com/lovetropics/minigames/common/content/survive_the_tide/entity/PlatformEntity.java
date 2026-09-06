package com.lovetropics.minigames.common.content.survive_the_tide.entity;

import com.lovetropics.minigames.LoveTropics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = LoveTropics.ID)
public class PlatformEntity extends Entity {
	private static final EntityDataAccessor<Integer> DATA_WIDTH_ID = SynchedEntityData.defineId(PlatformEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE = SynchedEntityData.defineId(PlatformEntity.class, EntityDataSerializers.BLOCK_STATE);

	private final InterpolationHandler interpolation = new InterpolationHandler(this, 3);

	private List<Vec3> riderOffsets = List.of();

	public PlatformEntity(EntityType<?> type, Level world) {
		super(type, world);
		noPhysics = true;
	}

	@SubscribeEvent
	public static void onEntityDismount(EntityMountEvent event) {
		if (event.isDismounting() && event.getEntityBeingMounted() instanceof PlatformEntity platform) {
			if (!platform.isRemoved()) {
				event.setCanceled(true);
			}
		}
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_WIDTH_ID, 1);
		builder.define(DATA_BLOCK_STATE, Blocks.CONCRETE.gray().defaultBlockState());
	}

	@Override
	public boolean canBeHitByProjectile() {
		return false;
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public PushReaction getPistonPushReaction() {
		return PushReaction.IGNORE;
	}

	@Override
	public boolean isIgnoringBlockTriggers() {
		return true;
	}

	@Override
	public void tick() {
		super.tick();
		boolean wasInterpolating = interpolation.hasActiveInterpolation();
		interpolation.interpolate();
		if (!level().isClientSide() && wasInterpolating && !interpolation.hasActiveInterpolation()) {
			solidifyAndRemove();
		}
	}

	private void solidifyAndRemove() {
		BlockState blockState = getBlockState();
		double downBias = 0.5;
		for (BlockPos pos : BlockPos.betweenClosed(
				BlockPos.containing(position().add(-getWidth() / 2.0f, -downBias, -getWidth() / 2.0f)),
				BlockPos.containing(position().add(getWidth() / 2.0f, -downBias, getWidth() / 2.0f))
		)) {
			level().setBlockAndUpdate(pos, blockState);
		}
		ejectPassengers();
		discard();
	}

	public void lerpTo(double x, double y, double z, int lerpLength) {
		interpolation.setInterpolationLength(lerpLength);
		interpolation.interpolateTo(new Vec3(x, y, z), 0.f, 0.0f);
	}

	@Override
	public @Nullable InterpolationHandler getInterpolation() {
		return interpolation;
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (DATA_WIDTH_ID.equals(key)) {
			refreshDimensions();
		}
	}

	public void setWidth(int width) {
		entityData.set(DATA_WIDTH_ID, width);
	}

	public int getWidth() {
		return entityData.get(DATA_WIDTH_ID);
	}

	public void setBlockState(BlockState blockState) {
		entityData.set(DATA_BLOCK_STATE, blockState);
	}

	public BlockState getBlockState() {
		return entityData.get(DATA_BLOCK_STATE);
	}

	private EntityDimensions getDimensions() {
		return EntityDimensions.scalable(getWidth(), 0.0f);
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		return getDimensions();
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		// Not persistent, nothing to save
	}

	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
		// Leave the player exactly where they were
		return passenger.position();
	}

	@Override
	protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
		Vec3 offset = getRiderOffset(getPassengers().indexOf(passenger));
		callback.accept(passenger, getX() + offset.x, getY() + offset.y, getZ() + offset.z);
	}

	private Vec3 getRiderOffset(int index) {
		if (index < 0) {
			return Vec3.ZERO;
		}
		if (index >= riderOffsets.size()) {
			float edgeBuffer = 1.0f;
			float width = getWidth() - edgeBuffer;
			RandomSource random = RandomSource.create(getId());
			riderOffsets = new ArrayList<>(index + 1);
			int failures = 0;
			while (riderOffsets.size() <= index) {
				float x = random.nextFloat() - 0.5f;
				float z = random.nextFloat() - 0.5f;
				Vec3 newOffset = new Vec3(x * width, 0.0f, z * width);
				if (isTooClose(newOffset) && failures++ < 100) {
					continue;
				}
				riderOffsets.add(newOffset);
			}
		}
		return riderOffsets.get(index);
	}

	private boolean isTooClose(Vec3 newOffset) {
		for (Vec3 otherOffset : riderOffsets) {
			if (otherOffset.closerThan(newOffset, 0.5)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource damageSource, float amount) {
		return false;
	}

	@Override
	public boolean shouldRiderSit() {
		return false;
	}
}
