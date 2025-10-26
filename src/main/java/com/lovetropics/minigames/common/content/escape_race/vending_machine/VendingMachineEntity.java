package com.lovetropics.minigames.common.content.escape_race.vending_machine;

import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
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
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class VendingMachineEntity extends Entity implements ContainerEntity {
	public static final List<VendingMachineSlot> SLOTS = List.of(
			new VendingMachineSlot(0.56f, 0.55f, -0.15f),
			new VendingMachineSlot(0.23f, 0.55f, -0.15f),
			new VendingMachineSlot(-0.1f, 0.55f, -0.15f),
			new VendingMachineSlot(-0.43f, 0.55f, -0.15f),
			new VendingMachineSlot(0.56f, 0.17f, -0.15f),
			new VendingMachineSlot(0.23f, 0.17f, -0.15f),
			new VendingMachineSlot(-0.1f, 0.17f, -0.15f),
			new VendingMachineSlot(-0.43f, 0.17f, -0.15f),
			new VendingMachineSlot(0.56f, -0.21f, -0.15f),
			new VendingMachineSlot(0.23f, -0.21f, -0.15f),
			new VendingMachineSlot(-0.1f, -0.21f, -0.15f),
			new VendingMachineSlot(-0.43f, -0.21f, -0.15f),
			new VendingMachineSlot(0.56f, -0.59f, -0.15f),
			new VendingMachineSlot(0.23f, -0.59f, -0.15f),
			new VendingMachineSlot(-0.1f, -0.59f, -0.15f),
			new VendingMachineSlot(-0.43f, -0.59f, -0.15f)
	);
	private static final EntityDataAccessor<NonNullList<ItemStack>> DATA_ITEMS = SynchedEntityData.defineId(VendingMachineEntity.class, EscapeRace.ITEM_STACK_LIST);
	private static final EntityDataAccessor<Integer> DATA_SELECTED = SynchedEntityData.defineId(VendingMachineEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_SELECTED_TICKS = SynchedEntityData.defineId(VendingMachineEntity.class, EntityDataSerializers.INT);
	private NonNullList<ItemStack> itemStacks;
	@Nullable
	private ResourceKey<LootTable> lootTable;
	private long lootTableSeed;
	public VendingMachineEntity(EntityType<? extends Entity> entityType, Level level) {
		super(entityType, level);
		this.itemStacks = NonNullList.withSize(36, ItemStack.EMPTY);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_ITEMS, NonNullList.withSize(36, ItemStack.EMPTY));
		builder.define(DATA_SELECTED, -1);
		builder.define(DATA_SELECTED_TICKS, -1);
	}

	@Override
	public void tick() {
		super.tick();
		if(!level().isClientSide()){
			ServerLevel level = (ServerLevel) level();
			//Server-side
			var nearestPlayer = level.getNearestPlayer(this, 2f);
			if(nearestPlayer != null) {
				if(nearestPlayer.hasLineOfSight(this)){
					Vec3 lookAngle = nearestPlayer.getLookAngle();
					lookAngle = lookAngle.scale(nearestPlayer.distanceTo(this));
					Vec3 target = nearestPlayer.getEyePosition().add(lookAngle);
					PoseStack poseStack = new PoseStack();
					poseStack.translate(position().x, position().y, position().z);
					poseStack.translate(0, 1.5, 0);
					poseStack.mulPose(Axis.YP.rotationDegrees(180f - this.getYRot()));
					int lookingAtIndex = -1;
					for (int i = 0; i < SLOTS.size(); i++) {
						poseStack.pushPose();
						VendingMachineSlot slot = SLOTS.get(i);
						poseStack.translate(slot.x, slot.y, slot.z);
						Vector3f vector3f = poseStack.last().pose().transformPosition(Vec3.ZERO.toVector3f(), new Vector3f());
						poseStack.popPose();
						Vec3 slotPosition = new Vec3(vector3f);
						double v = target.distanceTo(slotPosition);
						if(v <= 0.2){
							lookingAtIndex = i;
							break;
						}
					}
					if(lookingAtIndex != getEntityData().get(DATA_SELECTED)) {
						getEntityData().set(DATA_SELECTED, lookingAtIndex);
						getEntityData().set(DATA_SELECTED_TICKS, tickCount);
					}
				}
			}
		}
	}

	@Override
	public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float v) {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput valueInput) {
		this.readChestVehicleSaveData(valueInput);
		getEntityData().set(DATA_ITEMS, itemStacks);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput valueOutput) {
		this.addChestVehicleSaveData(valueOutput);
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
	protected AABB makeBoundingBox(Vec3 position) {
		return this.calculateBoundingBox(position);
	}

	private AABB calculateBoundingBox(Vec3 position) {
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

	@Override
	public boolean hasCustomOutlineRendering(Player player) {
		return true;
	}

	@Override
	public @org.jetbrains.annotations.Nullable ResourceKey<LootTable> getContainerLootTable() {
		return this.lootTable;
	}

	@Override
	public void setContainerLootTable(@org.jetbrains.annotations.Nullable ResourceKey<LootTable> resourceKey) {
		this.lootTable = resourceKey;
	}

	@Override
	public long getContainerLootTableSeed() {
		return this.lootTableSeed;
	}

	@Override
	public void setContainerLootTableSeed(long lootTableSeed) {
		this.lootTableSeed = lootTableSeed;
	}

	@Override
	public NonNullList<ItemStack> getItemStacks() {
		return itemStacks;
	}

	@Override
	public void clearItemStacks() {
		this.itemStacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
	}

	@Override
	public int getContainerSize() {
		return 36;
	}

	@Override
	public ItemStack getItem(int slot) {
		return this.getChestVehicleItem(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		return this.removeChestVehicleItem(slot, amount);
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return this.removeChestVehicleItemNoUpdate(slot);
	}

	@Override
	public void setItem(int slot, ItemStack itemStack) {
		this.setChestVehicleItem(slot, itemStack);
	}

	@Override
	public void setChanged() {
		this.getEntityData().set(DATA_ITEMS, this.itemStacks, true);
	}

	@Override
	public boolean stillValid(Player player) {
		return this.isChestVehicleStillValid(player);
	}

	@Override
	public void clearContent() {
		this.clearChestVehicleContent();
	}

	@Nullable
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player p_38253_) {
		if (this.lootTable != null && p_38253_.isSpectator()) {
			return null;
		} else {
			this.unpackChestVehicleLootTable(playerInventory.player);
			return ChestMenu.threeRows(containerId, playerInventory, this);
		}
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		player.openMenu(this);
		return InteractionResult.SUCCESS;
	}

	public NonNullList<ItemStack> getItems() {
		return this.getEntityData().get(DATA_ITEMS);
	}

	public int getSelected(){
		return this.getEntityData().get(DATA_SELECTED);
	}

	public int getSelectedTicks(){
		return this.getEntityData().get(DATA_SELECTED_TICKS);
	}

	public record VendingMachineSlot(float x, float y, float z) {}
}
