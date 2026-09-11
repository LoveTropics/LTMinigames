package com.lovetropics.minigames.common.content.escape_race.ddr_machine;

import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.content.escape_race.client.ddr.ClientDdrMachine;
import com.lovetropics.minigames.common.content.escape_race.client.ddr.DdrPlayerPoseState;
import com.lovetropics.minigames.common.content.escape_race.client.ddr.DdrScreen;
import com.lovetropics.minigames.common.content.escape_race.client.ddr.render.DDRMachineEntityRenderer;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DdrLevel;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DdrLevelInputQueue;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DdrRecordingSession;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DdrServerSession;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.TimedDdrInput;
import com.lovetropics.minigames.common.core.network.ddr.ClientboundDdrInputHitPacket;
import com.lovetropics.minigames.common.core.network.ddr.ServerboundSelectDdrLevelPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ByIdMap;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;

import org.jspecify.annotations.Nullable;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;

public class DDRMachineEntity extends Entity implements PlayerRideable {
	private static final int BED_COOLDOWN_TICKS = SharedConstants.TICKS_PER_SECOND * 2;

	private static final String IS_LOCKED = "is_locked";

	public enum DDRMachineState {
		MENU(0),
		PLAYING(1),
		POST_PLAY(2),
		RECORDING(3),
		BEDS(4);
		private final int id;

		DDRMachineState(int id) {
			this.id = id;
		}

		public int id() {
			return id;
		}

		private static final IntFunction<DDRMachineState> BY_ID = ByIdMap.continuous(
				DDRMachineState::id, values(), ByIdMap.OutOfBoundsStrategy.ZERO
		);
		public static final StreamCodec<ByteBuf, DDRMachineState> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, DDRMachineState::id);
	}

	public final AnimationState toBedState = new AnimationState();
	public final AnimationState toDDRMachineState = new AnimationState();

	private static final EntityDataAccessor<DdrInput> DATA_PLAYER_INPUT = SynchedEntityData.defineId(DDRMachineEntity.class, EscapeRace.DDR_INPUT);

	private static final EntityDataAccessor<DDRMachineState> DATA_STATE = SynchedEntityData.defineId(DDRMachineEntity.class, EscapeRace.DDR_STATE);
	private static final EntityDataAccessor<DdrSessionState> DATA_SESSION = SynchedEntityData.defineId(DDRMachineEntity.class, EscapeRace.DDR_SESSION);

	private final List<Holder<DdrLevel>> orderedLevels;

	private @Nullable DdrServerSession serverSession;
	private @Nullable DdrRecordingSession recordingSession;

	private @Nullable ClientDdrMachine clientMachine;

	private long canSwitchBedStateAfterTime;
	private boolean isLocked = false;

	public DDRMachineEntity(EntityType<? extends Entity> entityType, Level level) {
		super(entityType, level);
		orderedLevels = level.registryAccess().lookupOrThrow(EscapeRace.DDR_LEVEL).listElements()
				.sorted(Comparator.comparing(l -> l.key().identifier()))
				.map(l -> (Holder<DdrLevel>) l)
				.toList();

		if (level.isClientSide()) {
			clientMachine = new ClientDdrMachine();
		}

		toDDRMachineState.start(-BED_COOLDOWN_TICKS);
	}

	public List<Holder<DdrLevel>> getOrderedLevels() {
		return orderedLevels;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_PLAYER_INPUT, DdrInput.NONE)
				.define(DATA_STATE, DDRMachineState.MENU)
				.define(DATA_SESSION, DdrSessionState.INACTIVE);
	}

	@Override
	public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float v) {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		isLocked = input.getBooleanOr(IS_LOCKED, false);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putBoolean(IS_LOCKED, isLocked);
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
		return new AABB(position.x - 1.5f, position.y, position.z - 2f, position.x + 1.5f, position.y + 3f, position.z + 1.5f);
//		return AABB.ofSize(position, 3f, 3f, 4f);
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
//		foldIntoBedState.start(this.tickCount);
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}

		if (player.isPassengerOfSameVehicle(this)) {
			if (level().isClientSide()) {
				interactControlledClient(player);
			}
			return InteractionResult.SUCCESS;
		}

		if (!level().isClientSide() && !player.isShiftKeyDown()) {
			player.startRiding(this);
			return InteractionResult.SUCCESS;
		} else if (!level().isClientSide() && player.isShiftKeyDown() && !isLocked) {
			if (level().getGameTime() < canSwitchBedStateAfterTime) {
				return InteractionResult.FAIL;
			}
			ejectPassengers();
			canSwitchBedStateAfterTime = level().getGameTime() + BED_COOLDOWN_TICKS;
			if (getState() == DDRMachineState.MENU) {
				setState(DDRMachineState.BEDS);
			} else if (getState() == DDRMachineState.BEDS) {
				setState(DDRMachineState.MENU);
			}
		}

		return InteractionResult.PASS;
	}

	private void interactControlledClient(Player player) {
		if (!player.isLocalPlayer()) {
			return;
		}
		Holder<DdrLevel> pickedLevel = pickLevel(player);
		if (pickedLevel != null) {
			ClientPacketDistributor.sendToServer(new ServerboundSelectDdrLevelPacket(pickedLevel));
		}
	}

	public void setState(DDRMachineState newState) {
		getEntityData().set(DATA_STATE, newState);
	}

	@Override
	public @Nullable LivingEntity getControllingPassenger() {
		if (getFirstPassenger() instanceof LivingEntity passenger) {
			return passenger;
		}
		return null;
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		if (getState() == DDRMachineState.BEDS) {
			return getPassengers().size() < 2;
		}
		return getPassengers().isEmpty();
	}

	@Override
	protected Vec3 getPassengerAttachmentPoint(Entity entity, EntityDimensions dimensions, float partialTick) {
		if (getState() == DDRMachineState.BEDS) {
			int index = getPassengers().indexOf(entity);
			if (index == -1) {
				return super.getPassengerAttachmentPoint(entity, dimensions, partialTick);
			}
			return new Vec3(0f, 0.6f + index * 0.8f, 0.35f).yRot(-getYRot() * Mth.DEG_TO_RAD);
		}
		return super.getPassengerAttachmentPoint(entity, dimensions, partialTick);
	}

	private @Nullable Holder<DdrLevel> pickLevel(Player player) {
		if (!(player instanceof LocalPlayer)) {
			return null;
		}
		Minecraft minecraft = Minecraft.getInstance();
		EntityRenderDispatcher entityRenderDispatcher = minecraft.getEntityRenderDispatcher();
		if (!(entityRenderDispatcher.getRenderer(this) instanceof DDRMachineEntityRenderer renderer)) {
			return null;
		}
		DdrScreen screen = renderer.getScreen();
		int index = screen.pickLevelIndex(minecraft.gameRenderer.mainCamera(), position(), getYRot(), orderedLevels.size());
		return index != DdrScreen.NO_LEVEL_PICKED ? orderedLevels.get(index) : null;
	}

	@Override
	public boolean shouldRiderSit() {
		return false;
	}

	@Override
	public void tick() {
		super.tick();

		if (!level().isClientSide()) {
			tickServer();
		} else if (clientMachine != null) {
			clientMachine.tick(this);
		}

		if (level().isClientSide()) {
			DDRMachineState state = getState();
			toBedState.animateWhen(state == DDRMachineState.BEDS, tickCount);
			toDDRMachineState.animateWhen(state != DDRMachineState.BEDS, tickCount);
		}
	}

	private void tickServer() {
		if (getControllingPassenger() instanceof ServerPlayer player) {
			tickControlledServer(player);
		} else {
			serverSession = null;
			recordingSession = null;
		}
	}

	private void tickControlledServer(ServerPlayer player) {
		if (recordingSession != null) {
			DdrRecordingSession.Recording recording = recordingSession.tick(player);
			if (recording != null) {
				stopRecording(player, recording);
			}
		}

		if (serverSession != null && serverSession.isFinished(player)) {
			stopPlaying(player);
		}
	}

	public void startPlaying(ServerPlayer player, Holder<DdrLevel> level) {
		if (getState() != DDRMachineState.MENU) {
			return;
		}
		long startedAtTime = player.level().getGameTime();
		serverSession = new DdrServerSession(level, startedAtTime);
		player.sendSystemMessage(Component.literal("Starting track"), true);
		getEntityData().set(DATA_SESSION, DdrSessionState.playing(level, startedAtTime));
		setState(DDRMachineState.PLAYING);
	}

	@Override
	public void onRemovedFromLevel() {
		super.onRemovedFromLevel();
		if (clientMachine != null) {
			clientMachine.onRemoved();
		}
	}

	private void stopPlaying(ServerPlayer player) {
		if (serverSession == null) {
			return;
		}
		DDRScoreHelper.onGameFinished(player, serverSession.level(), serverSession.getCurrentLevelScore(), serverSession.getHighestStreak());
		player.sendSystemMessage(Component.literal("Your Score: " + serverSession.getCurrentLevelScore()));
		player.sendSystemMessage(Component.literal("Highest Streak: " + serverSession.getHighestStreak()));
		getEntityData().set(DATA_SESSION, DdrSessionState.INACTIVE);
		setState(DDRMachineState.MENU);
		serverSession = null;
		player.sendSystemMessage(Component.literal("Stopping track"));
	}

	public void startRecording(ServerPlayer player, Holder<JukeboxSong> track, String name) {
		if (getState() != DDRMachineState.MENU) {
			return;
		}
		long startedAtTime = player.level().getGameTime();
		recordingSession = new DdrRecordingSession(Component.literal(name), track, startedAtTime);
		player.sendSystemMessage(Component.literal("Starting track & recording..."));
		getEntityData().set(DATA_SESSION, DdrSessionState.recording(track, startedAtTime));
		setState(DDRMachineState.RECORDING);
	}

	public void stopRecording(ServerPlayer player) {
		if (recordingSession != null) {
			stopRecording(player, recordingSession.stopRecording());
		}
	}

	private void stopRecording(ServerPlayer player, DdrRecordingSession.Recording recording) {
		if (recordingSession == null) {
			return;
		}
		recordingSession = null;
		getEntityData().set(DATA_SESSION, DdrSessionState.INACTIVE);
		setState(DDRMachineState.MENU);

		player.sendSystemMessage(Component.literal("Stopping track & recording..."));

		recording.export(player.registryAccess());
	}

	public void handleClientInput(ServerPlayer player, DdrInput input, long inputTick) {
		DdrInput newInput = input.subtract(getPlayerInput());
		getEntityData().set(DATA_PLAYER_INPUT, input);
		if (serverSession != null) {
			DdrLevelInputQueue.Hit hit = serverSession.handleInputTick(player, newInput, inputTick);
			if (hit != null) {
				PacketDistributor.sendToPlayersTrackingEntity(this, new ClientboundDdrInputHitPacket(getId(), hit.hitTick()));
			}
		} else if (recordingSession != null) {
			recordingSession.handleInput(newInput, inputTick);
		}
	}

	public void handleRemoteClientInputHit(long inputTick) {
		if (clientMachine != null) {
			clientMachine.handleRemoteInputHit(this, inputTick);
		}
	}

	public DdrInput getPlayerInput() {
		return getEntityData().get(DATA_PLAYER_INPUT);
	}

	@Override
	public void onPassengerTurned(Entity entityToUpdate) {
		super.onPassengerTurned(entityToUpdate);
		entityToUpdate.setYBodyRot(180 + getYRot());
		if (getState() == DDRMachineState.PLAYING) {
			entityToUpdate.setYRot(180 + getYRot());
		}
	}

	@Override
	protected void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
		if (passenger instanceof ServerPlayer serverPlayer) {
			if (serverSession != null) {
				stopPlaying(serverPlayer);
			} else if (recordingSession != null) {
				stopRecording(serverPlayer);
			}
		} else if (clientMachine != null && passenger instanceof Player player && player.isLocalPlayer()) {
			clientMachine.onDismount();
		}
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (DATA_SESSION.equals(key) && clientMachine != null) {
			DdrSessionState session = getEntityData().get(DATA_SESSION);
			if (session.playingLevel().isPresent()) {
				clientMachine.startPlaying(this, session.playingLevel().get(), session.startedAtTime());
			} else if (session.recordingTrack().isPresent()) {
				clientMachine.startRecording(this, session.recordingTrack().get(), session.startedAtTime());
			} else {
				clientMachine.clearSession();
			}
		}
	}

	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
		return position().add(getDirection().getUnitVec3().scale((getBbWidth() + passenger.getBbWidth()) / 2.0f));
	}

	public DDRMachineState getState() {
		return getEntityData().get(DATA_STATE);
	}

	public Collection<TimedDdrInput> clientPendingInputs() {
		return Objects.requireNonNull(clientMachine).pendingInputs();
	}

	public long getClientCurrentTick() {
		return Objects.requireNonNull(clientMachine).getCurrentTick(this);
	}

	public DdrInput getClientCurrentInput() {
		return Objects.requireNonNull(clientMachine).getCurrentInput(this);
	}

	public DdrPlayerPoseState getPoseState() {
		return Objects.requireNonNull(clientMachine).poseState();
	}

	public boolean isPlayingSound() {
		return Objects.requireNonNull(clientMachine).isPlayingSound();
	}
}
