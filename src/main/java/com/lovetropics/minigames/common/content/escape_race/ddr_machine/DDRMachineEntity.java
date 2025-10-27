package com.lovetropics.minigames.common.content.escape_race.ddr_machine;

import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.content.escape_race.vending_machine.VendingMachineEntity;
import com.lovetropics.minigames.common.content.escape_race.vending_machine.VendingMachineEvents;
import com.lovetropics.minigames.common.core.game.IGameManager;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.network.ddr.UpdateDDRMachinePlayerPositionMessage;
import com.mojang.math.Axis;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.Commands;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideable;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class DDRMachineEntity extends Entity implements PlayerRideable {

	private static final int MAX_PASSENGERS = 1;
	public final AnimationState foldIntoBedState = new AnimationState();
	private static final EntityDataAccessor<Boolean> DATA_FORWARD = SynchedEntityData.defineId(DDRMachineEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_BACKWARD = SynchedEntityData.defineId(DDRMachineEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_LEFT = SynchedEntityData.defineId(DDRMachineEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_RIGHT = SynchedEntityData.defineId(DDRMachineEntity.class, EntityDataSerializers.BOOLEAN);

	private boolean isPlayerLeftLast = false;
	private boolean isPlayerRightLast = false;
	private boolean isPlayerForwardLast = false;
	private boolean isPlayerBackLast = false;

	public DDRMachineEntity(EntityType<? extends Entity> entityType, Level level) {
		super(entityType, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_FORWARD, false)
				.define(DATA_BACKWARD, false)
				.define(DATA_LEFT, false)
				.define(DATA_RIGHT, false);
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
	public boolean isPickable() {
		return true;
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
//		foldIntoBedState.start(this.tickCount);
		if (!level().isClientSide && !player.isShiftKeyDown()) {
			player.startRiding(this);
			return InteractionResult.SUCCESS;
		}

		return !player.isPassengerOfSameVehicle(this) ? InteractionResult.SUCCESS : InteractionResult.PASS;
	}


	@Override
	@Nullable
	public LivingEntity getControllingPassenger() {
		Entity firstPassenger = getFirstPassenger();
		LivingEntity controllingPassenger;
		if (firstPassenger instanceof LivingEntity passenger) {
			controllingPassenger = passenger;
		} else {
			controllingPassenger = null;
		}

		return controllingPassenger;
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return getPassengers().size() < MAX_PASSENGERS;
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float partialTick) {
		return new Vec3(0f, 0.6f, 0.25f);
//		return super.getPassengerAttachmentPoint(entity, dimensions, partialTick);
	}

	@Override
	public boolean shouldRiderSit() {
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		if(isLocalInstanceAuthoritative()) {
			if(level().isClientSide){
				handleControls();
			}
		}
	}

	@Override
	public void onPassengerTurned(Entity entityToUpdate) {
		super.onPassengerTurned(entityToUpdate);
		entityToUpdate.setYBodyRot(this.getYRot());
	}

	public void updatePlayerPosition(boolean left, boolean right, boolean forward, boolean backward) {
		getEntityData()
				.set(DATA_FORWARD, forward);
		getEntityData()
				.set(DATA_BACKWARD, backward);
		getEntityData()
				.set(DATA_LEFT, left);
		getEntityData()
				.set(DATA_RIGHT, right);
	}

	public boolean isPlayerLeft(){
		return getEntityData().get(DATA_LEFT);
	}
	public boolean isPlayerRight(){
		return getEntityData().get(DATA_RIGHT);
	}
	public boolean isPlayerForward(){
		return getEntityData().get(DATA_FORWARD);
	}
	public boolean isPlayerBack(){
		return getEntityData().get(DATA_BACKWARD);
	}


	private void handleControls(){
		if (this.isVehicle() && getControllingPassenger() instanceof final LocalPlayer localPlayer) {
			boolean inputLeft = localPlayer.input.keyPresses.left();
			boolean inputRight = localPlayer.input.keyPresses.right();
			boolean inputUp = localPlayer.input.keyPresses.forward();
			boolean inputDown = localPlayer.input.keyPresses.backward();
			if(inputLeft != isPlayerLeftLast || inputRight != isPlayerRightLast || inputUp != isPlayerForwardLast || inputDown != isPlayerBackLast) {
				ClientPacketDistributor.sendToServer(
						new UpdateDDRMachinePlayerPositionMessage(
								inputLeft,
								inputRight,
								inputUp,
								inputDown
						)
				);
				isPlayerLeftLast = inputLeft;
				isPlayerRightLast = inputRight;
				isPlayerForwardLast = inputUp;
				isPlayerBackLast = inputDown;
			}
		}
	}

}
