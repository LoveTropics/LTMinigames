package com.lovetropics.minigames.common.content.escape_race.ddr_machine;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.content.escape_race.vending_machine.VendingMachineEntity;
import com.lovetropics.minigames.common.core.network.ddr.SelectDDRMenuItemMessage;
import com.lovetropics.minigames.common.core.network.ddr.SetClientCameraViewMessage;
import com.lovetropics.minigames.common.core.network.ddr.UpdateDDRMachinePlayerPositionMessage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PlayerRideable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

public class DDRMachineEntity extends Entity implements PlayerRideable {
	public static final List<VendingMachineEntity.VendingMachineSlot> SLOTS = List.of(
			new VendingMachineEntity.VendingMachineSlot(0f, 0.5f, 0f),
			new VendingMachineEntity.VendingMachineSlot(0.5f, 0.5f, 0.5f),
			new VendingMachineEntity.VendingMachineSlot(1f, 0.5f, 0f),
			new VendingMachineEntity.VendingMachineSlot(1.5f, 0.5f, 0f),
			new VendingMachineEntity.VendingMachineSlot(0f, 0f, 0f),
			new VendingMachineEntity.VendingMachineSlot(0.5f, 0f, 0f),
			new VendingMachineEntity.VendingMachineSlot(1f, 0f, 0f),
			new VendingMachineEntity.VendingMachineSlot(1.5f, 0f, 0f)
	);
	public enum DDRMachineState {
		MENU(0),
		PLAYING(1),
		POST_PLAY(2),
		RECORDING(3);
		private final int id;

		DDRMachineState(int id) {
			this.id = id;
		}

		public int id(){
			return this.id;
		}

		private static final IntFunction<DDRMachineState> BY_ID = ByIdMap.continuous(
				DDRMachineState::id, values(), ByIdMap.OutOfBoundsStrategy.ZERO
		);
		public static final StreamCodec<ByteBuf, DDRMachineState> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, DDRMachineState::id);
	}

	private static final int MAX_PASSENGERS = 1;
	public final AnimationState foldIntoBedState = new AnimationState();
	private static final EntityDataAccessor<Boolean> DATA_FORWARD = SynchedEntityData.defineId(DDRMachineEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_BACKWARD = SynchedEntityData.defineId(DDRMachineEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_LEFT = SynchedEntityData.defineId(DDRMachineEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_RIGHT = SynchedEntityData.defineId(DDRMachineEntity.class, EntityDataSerializers.BOOLEAN);

	private static final EntityDataAccessor<List<DDRMachineLevelClient>> DATA_LEVELS = SynchedEntityData.defineId(DDRMachineEntity.class, EscapeRace.DDR_LEVEL_LIST);
	private static final EntityDataAccessor<DDRMachineState> DATA_STATE = SynchedEntityData.defineId(DDRMachineEntity.class, EscapeRace.DDR_STATE);
	private static final EntityDataAccessor<Map<Integer, DDRMachineLevelTick>> DATA_UPCOMING_MOVES = SynchedEntityData.defineId(DDRMachineEntity.class, EscapeRace.DDR_LEVEL_TICK_MAP);
	private static final EntityDataAccessor<Integer> DATA_CURRENT_TICK = SynchedEntityData.defineId(DDRMachineEntity.class, EntityDataSerializers.INT);

	private boolean isPlayerLeftLast = false;
	private boolean isPlayerRightLast = false;
	private boolean isPlayerForwardLast = false;
	private boolean isPlayerBackLast = false;

	private DDRMachineState state = DDRMachineState.MENU;

	private int recordingStartTick = 0;

	private DDRMachineLevel currentLevel = null;
	private int currentLevelLength = 0;

	private int playingStartTick = 0;

	public DDRMachineEntity(EntityType<? extends Entity> entityType, Level level) {
		super(entityType, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_FORWARD, false)
				.define(DATA_BACKWARD, false)
				.define(DATA_LEFT, false)
				.define(DATA_RIGHT, false)
				.define(DATA_LEVELS, DDRMachineLevels.REGISTRY.stream().map(DDRMachineLevel::toClient).toList())
				.define(DATA_STATE, DDRMachineState.MENU)
				.define(DATA_UPCOMING_MOVES, new HashMap<>())
				.define(DATA_CURRENT_TICK, 0);
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
			getEntityData().set(DATA_LEVELS, DDRMachineLevels.REGISTRY.stream().map(DDRMachineLevel::toClient).toList());
			player.startRiding(this);
			return InteractionResult.SUCCESS;
		}

		return !player.isPassengerOfSameVehicle(this) ? InteractionResult.SUCCESS : InteractionResult.PASS;
	}


	@Override
	public boolean hasCustomOutlineRendering(Player player) {
		return true;
	}

	public void setState(DDRMachineState state) {
		this.state = state;
		getEntityData().set(DATA_STATE, state);
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
	public boolean hurtClient(DamageSource damageSource) {
		if(damageSource.getEntity() instanceof Player player){
			if(player.hasLineOfSight(this) && player.getLookAngle().dot(this.getLookAngle()) < 1){
				int lookingAtIndex = calculatePlayerLookingAtSlot(player);
				if(lookingAtIndex != -1){
					ClientPacketDistributor.sendToServer(new SelectDDRMenuItemMessage(this.getId(), lookingAtIndex));
				}
//				tryPurchase(player, entityData.get(DATA_SELECTED));
			}
		}
		return super.hurtClient(damageSource);
	}

	public int calculatePlayerLookingAtSlot(Player player) {
		Vec3 lookAngle = player.getLookAngle();
		lookAngle = lookAngle.scale(5f);
		Vec3 target = player.getEyePosition().add(lookAngle);
		PoseStack poseStack = new PoseStack();
		poseStack.translate(position().x, position().y, position().z);
		poseStack.translate(0, 1.5, 0);
		poseStack.mulPose(Axis.YP.rotationDegrees(180f - this.getYRot()));
//		poseStack.mulPose(Axis.YP.rotationDegrees(-90F));
		poseStack.translate(-0.05, 0, -1.7f);
		poseStack.translate(-0.7f, -0.4,0f);
//		poseStack.scale(0.3f, 0.3f, 0.3f);
		int lookingAtIndex = -1;
		for (int i = 0; i < SLOTS.size(); i++) {
			poseStack.pushPose();
			VendingMachineEntity.VendingMachineSlot slot = SLOTS.get(i);
			poseStack.translate(slot.x(), slot.y(), slot.z());
			Vector3f vector3f = poseStack.last().pose().transformPosition(Vec3.ZERO.toVector3f(), new Vector3f());
			poseStack.popPose();
			Vec3 slotPosition = new Vec3(vector3f);
			AABB aabb = AABB.ofSize(slotPosition, 0.2, 0.2, 0.2);
			Optional<Vec3> clip = aabb.clip(player.getEyePosition(), target);
			if (clip.isPresent()) {
				lookingAtIndex = i;
				break;
			}
		}
		return lookingAtIndex;
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
				calculatePlayerLookingAtSlot(Minecraft.getInstance().player);
			}
			return;
		}
		if(!level().isClientSide){
			if(state == DDRMachineState.RECORDING && currentLevel != null) {
				int currentTick = tickCount - recordingStartTick;
				boolean hasChanged = false;
				boolean left= false, right= false, forward = false, back = false;
				if(isPlayerLeft() && !isPlayerLeftLast){
					hasChanged = true;
					left = true;
					// Record left;
				}
				if(isPlayerRight() && !isPlayerRightLast){
					hasChanged = true;
					right = true;
				}
				if(isPlayerForward() && !isPlayerForwardLast){
					hasChanged = true;
					forward = true;
				}
				if(isPlayerBack() && !isPlayerBackLast){
					hasChanged = true;
					back = true;
				}
				if(hasChanged) {
					DDRMachineLevelTick tick = new DDRMachineLevelTick(forward, left, back, right);
					currentLevel.ticks().put(currentTick + "", tick);
					this.updateLast();
				}
				if(currentTick >= currentLevelLength) {
					stopRecording();
				}
			} else if(state == DDRMachineState.PLAYING && currentLevel != null) {
				int currentTick = tickCount - playingStartTick;
				getEntityData().set(DATA_CURRENT_TICK, currentTick);
				Map<Integer, DDRMachineLevelTick> collect = currentLevel.ticks().entrySet().stream()
						.filter((entry) -> {
							int tick = Integer.parseInt(entry.getKey());
							return tick >= currentTick && tick - currentTick <= 20 * 4;
						} )
						.collect(Collectors.toMap((entry) -> Integer.parseInt(entry.getKey()), Map.Entry::getValue));
				getEntityData().set(DATA_UPCOMING_MOVES, collect);
				if(currentLevel.ticks().containsKey(currentTick + "")) {
					DDRMachineLevelTick tick = currentLevel.ticks().get(currentTick + "");
					if(getControllingPassenger() instanceof ServerPlayer player){
					}
				}
				if(currentTick >= currentLevelLength) {
					stopPlaying();
				}
			}
		}
	}

	public void startPlaying(DDRMachineLevel level){
		if(state == DDRMachineState.PLAYING && currentLevel != null) {
			return;
		}
		playingStartTick = tickCount;
		currentLevel = level;
		JukeboxSong value = level().registryAccess().get(currentLevel.track()).get().value();
		currentLevelLength = value.lengthInTicks();
		if(getControllingPassenger() instanceof ServerPlayer player){
			player.connection.send(new SetClientCameraViewMessage(1));
			int i = level().registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG).getId(value);
			level().levelEvent(null, 1010, BlockPos.containing(this.position()), i);
			player.sendSystemMessage(Component.literal("Starting track"));
		}
		setState(DDRMachineState.PLAYING);
	}

	public void stopPlaying(){
		if(getControllingPassenger() instanceof ServerPlayer player) {
			stopPlaying(player);
		}
	}

	public void stopPlaying(ServerPlayer player){
		if(state != DDRMachineState.PLAYING || currentLevel == null) {
			return;
		}
		setState(DDRMachineState.MENU);
		playingStartTick = 0;
		currentLevel = null;
		level().levelEvent(1011, BlockPos.containing(this.position()), 0);
		player.sendSystemMessage(Component.literal("Stopping track"));
		player.connection.send(new SetClientCameraViewMessage(0));
	}

	public void startRecording(ResourceKey<JukeboxSong> track, String name){
		if(state == DDRMachineState.RECORDING && currentLevel != null) {
			return;
		}
		recordingStartTick = tickCount;
		currentLevel = new DDRMachineLevel(LoveTropics.location(name), track, name, Component.literal(name), new HashMap<>());
		JukeboxSong value = level().registryAccess().get(currentLevel.track()).get().value();
		currentLevelLength = value.lengthInTicks();
		if(getControllingPassenger() instanceof ServerPlayer player){
			int i = level().registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG).getId(value);
			level().levelEvent(null, 1010, BlockPos.containing(this.position()), i);
			player.sendSystemMessage(Component.literal("Starting track & recording..."));
		}
		setState(DDRMachineState.RECORDING);
	}

	public void stopRecording(){
		if(state != DDRMachineState.RECORDING || currentLevel == null) {
			return;
		}
		setState(DDRMachineState.MENU);
		recordingStartTick = 0;
		if(getControllingPassenger() instanceof ServerPlayer player){
			level().levelEvent(1011, BlockPos.containing(this.position()), 0);
			player.sendSystemMessage(Component.literal("Stopping track & recording..."));
		}
		getServer().submit(() -> {
			DDRMachineLevel.codec(currentLevel.id())
					.encodeStart(JsonOps.INSTANCE, currentLevel)
					.ifError((error) -> {
						System.out.println(error.message());
					}).ifSuccess((output) -> {
						try {
							Path path = DDRMachineLevel.pathFor(LoveTropics.location(currentLevel.track().location().getPath()));
							Files.createDirectories(path.getParent());
							Files.deleteIfExists(path);
							Files.writeString(path, output.toString());
						} catch (IOException e) {
							e.printStackTrace();
						}
					});
		});
	}

	@Override
	public void onPassengerTurned(Entity entityToUpdate) {
		super.onPassengerTurned(entityToUpdate);
		entityToUpdate.setYBodyRot(this.getYRot());
	}

	private void updateLast(){
		isPlayerLeftLast = isPlayerLeft();
		isPlayerRightLast = isPlayerRight();
		isPlayerForwardLast = isPlayerForward();
		isPlayerBackLast = isPlayerBack();
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

	@Override
	protected void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
		if(!level().isClientSide && passenger instanceof ServerPlayer serverPlayer) {
			if(state == DDRMachineState.PLAYING) {
				stopPlaying(serverPlayer);
			}
		}
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

	public List<DDRMachineLevelClient> getAvailableLevels(){
		return this.getEntityData().get(DATA_LEVELS);
	}

	public DDRMachineState getState(){
		return getEntityData().get(DATA_STATE);
	}

	public Map<Integer, DDRMachineLevelTick> getUpcomingMoves(){
		return getEntityData().get(DATA_UPCOMING_MOVES);
	}

	public int getCurrentTick(){
		return getEntityData().get(DATA_CURRENT_TICK);
	}

}
