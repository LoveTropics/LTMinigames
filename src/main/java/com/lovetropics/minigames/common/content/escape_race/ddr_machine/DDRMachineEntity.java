package com.lovetropics.minigames.common.content.escape_race.ddr_machine;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DDRMachineLevelState;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DdrLevel;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.TimedDdrInput;
import com.lovetropics.minigames.common.content.escape_race.vending_machine.VendingMachineEntity;
import com.lovetropics.minigames.common.core.network.ddr.ServerboundSelectDdrLevelPacket;
import com.lovetropics.minigames.common.core.network.ddr.ClientboundSetCameraViewPacket;
import com.lovetropics.minigames.common.core.network.ddr.ServerboundUpdateDdrInputPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
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
import java.util.Comparator;
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

		public int id() {
			return this.id;
		}

		private static final IntFunction<DDRMachineState> BY_ID = ByIdMap.continuous(
				DDRMachineState::id, values(), ByIdMap.OutOfBoundsStrategy.ZERO
		);
		public static final StreamCodec<ByteBuf, DDRMachineState> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, DDRMachineState::id);
	}

	private static final int MAX_PASSENGERS = 1;
	public final AnimationState foldIntoBedState = new AnimationState();

	private static final EntityDataAccessor<DdrInput> DATA_PLAYER_INPUT = SynchedEntityData.defineId(DDRMachineEntity.class, EscapeRace.DDR_INPUT);

	private static final EntityDataAccessor<DDRMachineState> DATA_STATE = SynchedEntityData.defineId(DDRMachineEntity.class, EscapeRace.DDR_STATE);
	private static final EntityDataAccessor<List<TimedDdrInput>> DATA_UPCOMING_MOVES = SynchedEntityData.defineId(DDRMachineEntity.class, EscapeRace.DDR_LEVEL_TICK_MAP);
	private static final EntityDataAccessor<Integer> DATA_CURRENT_TICK = SynchedEntityData.defineId(DDRMachineEntity.class, EntityDataSerializers.INT);

	private final List<Holder<DdrLevel>> orderedLevels;

	private DdrInput lastInput = DdrInput.NONE;
	private final DdrPlayerPoseState poseState = new DdrPlayerPoseState();

	private int recordingStartTick = 0;

	@Nullable
	private DDRMachineLevelState currentLevelState = null;
	private int currentLevelLength = 0;

	private int playingStartTick = 0;

	public DDRMachineEntity(EntityType<? extends Entity> entityType, Level level) {
		super(entityType, level);
		orderedLevels = level.registryAccess().lookupOrThrow(EscapeRace.DDR_LEVEL).listElements()
				.sorted(Comparator.comparing(l -> l.key().location()))
				.map(l -> (Holder<DdrLevel>) l)
				.toList();
	}

	public List<Holder<DdrLevel>> getOrderedLevels() {
		return orderedLevels;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_PLAYER_INPUT, DdrInput.NONE)
				.define(DATA_STATE, DDRMachineState.MENU)
				.define(DATA_UPCOMING_MOVES, List.of())
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
		getEntityData().set(DATA_STATE, state);
	}

	@Override
	@Nullable
	public LivingEntity getControllingPassenger() {
		if (getFirstPassenger() instanceof LivingEntity passenger) {
			return passenger;
		}
		return null;
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
		if (damageSource.getEntity() instanceof Player player) {
			if (player.hasLineOfSight(this) && player.getLookAngle().dot(this.getLookAngle()) < 1) {
				List<Holder<DdrLevel>> levels = getOrderedLevels();
				int lookingAtIndex = calculatePlayerLookingAtSlot(player);
				if (lookingAtIndex >= 0 && lookingAtIndex < levels.size()) {
					ClientPacketDistributor.sendToServer(new ServerboundSelectDdrLevelPacket(this.getId(), levels.get(lookingAtIndex)));
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
		poseStack.translate(-0.7f, -0.4, 0f);
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
		if (isLocalInstanceAuthoritative()) {
			if (level().isClientSide) {
				handleControls();
				poseState.tick(lastInput);
			}
			return;
		} else if (level().isClientSide()) {
			poseState.tick(getPlayerInput());
		}
		if (!level().isClientSide) {
			DdrInput newInputs = getPlayerInput().subtract(lastInput);
			lastInput = getPlayerInput();

			if (getState() == DDRMachineState.RECORDING && currentLevelState != null) {
				int currentTick = tickCount - recordingStartTick;
				if (!newInputs.isEmpty()) {
					currentLevelState.getLevel().value().ticks().put(currentTick, newInputs);
				}
				if (currentTick >= currentLevelLength) {
					stopRecording();
				}
			} else if (getState() == DDRMachineState.PLAYING && currentLevelState != null) {
				int currentTick = tickCount - playingStartTick;
				getEntityData().set(DATA_CURRENT_TICK, currentTick);
				getEntityData().set(DATA_UPCOMING_MOVES, currentLevelState.pendingInputs()
						.filter(input -> {
							long tick = input.tick();
							return (tick + 10) >= currentTick && tick - currentTick <= 20 * 4;
						})
						.toList());
				currentLevelState.checkIfHit(currentTick, newInputs, (ServerPlayer) getControllingPassenger());
				if (currentTick >= currentLevelLength) {
					stopPlaying();
				}
			}
		}
	}

	public void startPlaying(Holder<DdrLevel> level) {
		if (getState() == DDRMachineState.PLAYING && currentLevelState != null) {
			return;
		}
		playingStartTick = tickCount;
		currentLevelState = new DDRMachineLevelState(level);
		Holder<JukeboxSong> track = currentLevelState.getLevel().value().track();
		currentLevelLength = track.value().lengthInTicks();
		if (getControllingPassenger() instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetCameraViewPacket(1));
			playSong(track);
			player.sendSystemMessage(Component.literal("Starting track"), true);
		}
		setState(DDRMachineState.PLAYING);
	}

	private void playSong(Holder<JukeboxSong> track) {
		int songId = level().registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG).getId(track.value());
		level().levelEvent(null, LevelEvent.SOUND_PLAY_JUKEBOX_SONG, blockPosition(), songId);
	}

	public void stopPlaying() {
		if (getControllingPassenger() instanceof ServerPlayer player) {
			stopPlaying(player);
		}
	}

	public void stopPlaying(ServerPlayer player) {
		if (getState() != DDRMachineState.PLAYING || currentLevelState == null) {
			return;
		}
		setState(DDRMachineState.MENU);
		DDRScoreHelper.onGameFinished(player, currentLevelState.getLevel().value().track(), currentLevelState.getCurrentLevelScore(), currentLevelState.getHighestStreak());
		player.sendSystemMessage(Component.literal("Your Score: " + currentLevelState.getCurrentLevelScore()));
		player.sendSystemMessage(Component.literal("Highest Streak: " + currentLevelState.getHighestStreak()));
		playingStartTick = 0;
		currentLevelState = null;
		level().levelEvent(LevelEvent.SOUND_STOP_JUKEBOX_SONG, blockPosition(), 0);
		player.sendSystemMessage(Component.literal("Stopping track"));
		player.connection.send(new ClientboundSetCameraViewPacket(0));
	}

	public void startRecording(Holder<JukeboxSong> track, String name) {
		if (getState() == DDRMachineState.RECORDING && currentLevelState != null) {
			return;
		}
		recordingStartTick = tickCount;
		currentLevelState = new DDRMachineLevelState(Holder.direct(new DdrLevel(track, new ItemStack(Items.MUSIC_DISC_PIGSTEP), Component.literal(name), new Long2ObjectOpenHashMap<>())));
		currentLevelLength = track.value().lengthInTicks();
		if (getControllingPassenger() instanceof ServerPlayer player) {
			playSong(track);
			player.sendSystemMessage(Component.literal("Starting track & recording..."));
		}
		setState(DDRMachineState.RECORDING);
	}

	public void stopRecording() {
		if (getState() != DDRMachineState.RECORDING || currentLevelState == null) {
			return;
		}
		setState(DDRMachineState.MENU);
		recordingStartTick = 0;
		if (getControllingPassenger() instanceof ServerPlayer player) {
			level().levelEvent(LevelEvent.SOUND_STOP_JUKEBOX_SONG, blockPosition(), 0);
			player.sendSystemMessage(Component.literal("Stopping track & recording..."));
		}
		Holder<DdrLevel> level = currentLevelState.getLevel();
		getServer().submit(() -> {
			DdrLevel.DIRECT_CODEC.encodeStart(registryAccess().createSerializationContext(JsonOps.INSTANCE), level.value())
					.ifError((error) -> {
						System.out.println(error.message());
					}).ifSuccess((output) -> {
						try {
							Path path = DdrLevel.pathFor(LoveTropics.location(level.value().track().unwrapKey().orElseThrow().location().getPath()));
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
		if (getState() == DDRMachineState.PLAYING) {
			entityToUpdate.setYRot(this.getYRot());
		}
	}

	public void updatePlayerInput(DdrInput pose) {
		getEntityData().set(DATA_PLAYER_INPUT, pose);
	}

	public DdrInput getPlayerInput() {
		return getEntityData().get(DATA_PLAYER_INPUT);
	}

	@Override
	protected void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
		if (!level().isClientSide && passenger instanceof ServerPlayer serverPlayer) {
			if (getState() == DDRMachineState.PLAYING) {
				stopPlaying(serverPlayer);
			}
		}
	}

	private void handleControls() {
		if (isVehicle() && getControllingPassenger() instanceof final LocalPlayer player) {
			Input keyPresses = player.input.keyPresses;
			DdrInput input = DdrInput.fromKeyPresses(keyPresses);
			if (!input.equals(lastInput)) {
				ClientPacketDistributor.sendToServer(new ServerboundUpdateDdrInputPacket(input));
				lastInput = input;
			}
		}
	}

	public DDRMachineState getState() {
		return getEntityData().get(DATA_STATE);
	}

	public List<TimedDdrInput> getUpcomingMoves() {
		return getEntityData().get(DATA_UPCOMING_MOVES);
	}

	public int getCurrentTick() {
		return getEntityData().get(DATA_CURRENT_TICK);
	}

	public DdrPlayerPoseState getPoseState() {
		return poseState;
	}
}
