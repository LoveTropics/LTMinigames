package com.lovetropics.minigames.common.content.escape_race.vending_machine;

import com.google.common.collect.Lists;
import com.lovetropics.minigames.SoundRegistry;
import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.impl.GamePhaseManager;
import com.lovetropics.minigames.common.core.network.vending.ClientboundVendingMachineDropPacket;
import com.lovetropics.minigames.common.core.network.vending.SelectVendingMachineItemMessage;
import com.lovetropics.minigames.common.core.network.vending.ServerboundVendingMachinePurchasePacket;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import javax.sound.sampled.Port;
import java.util.List;

public class VendingMachineEntity extends Entity implements ContainerEntity {
	private static final EntityDataAccessor<List<ItemStack>> DATA_VISUAL_ITEMS = SynchedEntityData.defineId(VendingMachineEntity.class, EscapeRace.ITEM_STACK_LIST);
	private static final EntityDataAccessor<Integer> DATA_SELECTED = SynchedEntityData.defineId(VendingMachineEntity.class, EntityDataSerializers.INT);

	public static final int NO_SLOT = -1;
	private static final int SELECT_COOLDOWN = SharedConstants.TICKS_PER_SECOND * 2;

	public static final int DROP_ITEM_TICKS = SharedConstants.TICKS_PER_SECOND * 3 / 2;

	public static final int INVENTORY_SIZE = Math.max(Mth.roundToward(VendingMachineSlots.COUNT, 9), 27);

	private final NonNullList<ItemStack> itemStacks = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
	@Nullable
	private ResourceKey<LootTable> lootTable;
	private long lootTableSeed;

	@Nullable
	private ItemStack droppingItem = ItemStack.EMPTY;
	private int droppingFromSlot = NO_SLOT;
	private int droppingItemTicks;

	private long lastSelectionTime;

	public VendingMachineEntity(EntityType<? extends Entity> entityType, Level level) {
		super(entityType, level);
	}

	private static ItemStack copyVisualStack(ItemStack itemStack) {
		if (itemStack.isEmpty()) {
			return ItemStack.EMPTY;
		}
		ItemStack visualStack = itemStack.copyWithCount(1);
		// Does this actually matter? Probably not. But we can do it!
		visualStack.remove(DataComponents.CONTAINER);
		visualStack.remove(DataComponents.WRITTEN_BOOK_CONTENT);
		return visualStack;
	}

	private void onItemsChanged() {
		getEntityData().set(DATA_VISUAL_ITEMS, List.copyOf(Lists.transform(itemStacks.subList(0, VendingMachineSlots.COUNT), VendingMachineEntity::copyVisualStack)));
		int selected = getSelected();
		if (selected != NO_SLOT && itemStacks.get(selected).isEmpty()) {
			getEntityData().set(DATA_SELECTED, NO_SLOT);
		}
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_VISUAL_ITEMS, List.of());
		builder.define(DATA_SELECTED, NO_SLOT);
	}

	@Override
	public void tick() {
		super.tick();

		if (droppingItem != null) {
			droppingItemTicks++;
			if (droppingItemTicks >= DROP_ITEM_TICKS) {
				if (!level().isClientSide()) {
					spawnDroppedItem(droppingItem);
				}
				droppingItem = null;
				droppingFromSlot = NO_SLOT;
				droppingItemTicks = 0;
			}
		}
	}

	private void spawnDroppedItem(ItemStack itemStack) {
		Vec3 forward = getDirection().getUnitVec3();
		Vec3 spawnPos = position().add(0.0, 0.25, 0.0).add(forward.scale(0.75f));
		ItemEntity entity = new ItemEntity(level(), spawnPos.x, spawnPos.y, spawnPos.z, itemStack.copy());
		entity.setDeltaMovement(forward.scale(0.1).add(0.0, 0.2, 0.0));
		level().addFreshEntity(entity);
	}

	public void tryPurchase(ServerPlayer player, int itemIndex) {
		if (droppingItem != null) {
			return;
		}
		ItemStack selectedItem = getItem(itemIndex);
		if (selectedItem.isEmpty()) {
			return;
		}
		IGamePhase game = GamePhaseManager.get().getGamePhaseFor(player);
		if (game != null && game.invoker(VendingMachineEvents.PURCHASE_ITEM).tryPurchaseItem(player, this, selectedItem).isFalse()) {
			playSound(SoundRegistry.INCORRECT.value());
			return;
		}
		playSound(SoundRegistry.CORRECT.value());
		startDropping(selectedItem.copyWithCount(1), itemIndex);
		getEntityData().set(DATA_SELECTED, NO_SLOT);
	}

	@Override
	public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float v) {
		return false;
	}

	public void trySelect(int index) {
		if (index < 0 || index >= VendingMachineSlots.COUNT) {
			return;
		}
		long gameTime = level().getGameTime();
		if (gameTime - lastSelectionTime < SELECT_COOLDOWN) {
			return;
		}
		if (index != getEntityData().get(DATA_SELECTED)) {
			getEntityData().set(DATA_SELECTED, index);
			lastSelectionTime = gameTime;
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput valueInput) {
		readChestVehicleSaveData(valueInput);
		onItemsChanged();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput valueOutput) {
		addChestVehicleSaveData(valueOutput);
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
		return calculateBoundingBox(position);
	}

	private AABB calculateBoundingBox(Vec3 position) {
		var facing = getDirection();
		Vec3 vec31 = position.relative(facing, 0.5f);
		Vec3 vec32 = position.relative(facing.getOpposite(), 0.5f);
		for (Direction.Axis value : Direction.Axis.values()) {
			if (value == facing.getAxis() || value == Direction.Axis.Y) {
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
		return lootTable;
	}

	@Override
	public void setContainerLootTable(@org.jetbrains.annotations.Nullable ResourceKey<LootTable> resourceKey) {
		lootTable = resourceKey;
	}

	@Override
	public long getContainerLootTableSeed() {
		return lootTableSeed;
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
		itemStacks.clear();
	}

	@Override
	public int getContainerSize() {
		return INVENTORY_SIZE;
	}

	@Override
	public ItemStack getItem(int slot) {
		return getChestVehicleItem(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		return removeChestVehicleItem(slot, amount);
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return removeChestVehicleItemNoUpdate(slot);
	}

	@Override
	public void setItem(int slot, ItemStack itemStack) {
		setChestVehicleItem(slot, itemStack);
	}

	@Override
	public void setChanged() {
		onItemsChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return isChestVehicleStillValid(player);
	}

	@Override
	public void clearContent() {
		clearChestVehicleContent();
	}

	@Override
	@Nullable
	public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
		if (lootTable != null && player.isSpectator()) {
			return null;
		} else {
			unpackChestVehicleLootTable(playerInventory.player);
			return ChestMenu.threeRows(containerId, playerInventory, this);
		}
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
		if (player.isShiftKeyDown()) {
			if (player.canUseGameMasterBlocks()) {
				player.openMenu(this);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.FAIL;
		}

		if (level().isClientSide()) {
			interactClient(player);
		}
		return InteractionResult.SUCCESS;
	}

	private void interactClient(Player player) {
		if (!player.isLocalPlayer()) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		Camera camera = minecraft.gameRenderer.mainCamera();
		VendingMachineEntityRenderer renderer = (VendingMachineEntityRenderer) minecraft.getEntityRenderDispatcher().getRenderer(this);
		VendingMachineSlots.Picker picker = VendingMachineSlots.picker(camera, this);
		int pickedSlot = picker.pickSlot();
		if (pickedSlot != NO_SLOT) {
			ClientPacketDistributor.sendToServer(new SelectVendingMachineItemMessage(getId(), pickedSlot));
		} else if (picker.isPicked(renderer.getModel().buyButtonBounds())) {
			ClientPacketDistributor.sendToServer(new ServerboundVendingMachinePurchasePacket(getId()));
		}
	}

	public List<ItemStack> getVisualItems() {
		return getEntityData().get(DATA_VISUAL_ITEMS);
	}

	public int getSelected() {
		return getEntityData().get(DATA_SELECTED);
	}

	public void startDropping(ItemStack itemStack, int fromSlot) {
		droppingItem = itemStack;
		droppingFromSlot = fromSlot;
		droppingItemTicks = 0;
		if (!level().isClientSide()) {
			PacketDistributor.sendToPlayersTrackingEntity(this, new ClientboundVendingMachineDropPacket(getId(), itemStack.copy(), fromSlot));
		}
	}

	@Nullable
	public ItemStack getDroppingItem() {
		return droppingItem;
	}

	public int getDroppingFromSlot() {
		return droppingFromSlot;
	}

	public float getDroppingItemProgress(float partialTicks) {
		if (droppingItem == null) {
			return 0.0f;
		}
		return Math.min(droppingItemTicks + partialTicks, DROP_ITEM_TICKS) / DROP_ITEM_TICKS;
	}
}
