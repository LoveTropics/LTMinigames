package com.lovetropics.minigames.common.content.escape_race.client.ddr;

import com.lovetropics.minigames.common.content.escape_race.client.ddr.sound.DdrSoundInstance;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DDRMachineEntity;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DdrInput;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DdrLevel;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DdrLevelInputQueue;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DdrServerSession;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.TimedDdrInput;
import com.lovetropics.minigames.common.core.network.ddr.ServerboundDdrInputPacket;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ClientDdrMachine {
	private static final double SOUND_RANGE = 5.0;

	@Nullable
	private Session session;

	private DdrInput lastInput = DdrInput.NONE;

	private final DdrPlayerPoseState poseState = new DdrPlayerPoseState();
	@Nullable
	private DdrSoundInstance playingTrackSound;

	public void tick(DDRMachineEntity entity) {
		if (entity.getControllingPassenger() instanceof LocalPlayer) {
			tickControlledLocal(entity);
			poseState.tick(lastInput);
		} else {
			lastInput = DdrInput.NONE;
			poseState.tick(entity.getPlayerInput());
		}

		if (session != null) {
			session.tick(entity);
		}

		tickSound(entity);
	}

	private void tickControlledLocal(DDRMachineEntity entity) {
		Options options = Minecraft.getInstance().options;
		DdrInput input = new DdrInput(
				options.keyUp.isDown(),
				options.keyDown.isDown(),
				options.keyLeft.isDown(),
				options.keyRight.isDown()
		);
		DdrInput newInput = input.subtract(lastInput);
		lastInput = input;

		long currentTick = getCurrentTick(entity);
		ClientPacketDistributor.sendToServer(new ServerboundDdrInputPacket(input, currentTick));

		if (session != null) {
			session.handleLocalInput(newInput, currentTick);
		}
	}

	public void handleRemoteInputHit(DDRMachineEntity entity, long inputTick) {
		if (session != null) {
			session.handleRemoteInputHit(entity, inputTick);
		}
	}

	public void startPlaying(DDRMachineEntity entity, Holder<DdrLevel> level, long startedAtTime) {
		if (entity.getControllingPassenger() instanceof LocalPlayer) {
			Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK);
		}
		session = new PlaySession(level, startedAtTime);
	}

	public void startRecording(DDRMachineEntity entity, Holder<JukeboxSong> track, long startedAtTime) {
		if (entity.getControllingPassenger() instanceof LocalPlayer) {
			Minecraft.getInstance().options.setCameraType(CameraType.THIRD_PERSON_BACK);
		}
		session = new RecordingSession(track, startedAtTime);
	}

	public void clearSession() {
		session = null;
	}

	public void onDismount() {
		Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
	}

	public Collection<TimedDdrInput> pendingInputs() {
		return session != null ? session.pendingInputs() : List.of();
	}

	public long getCurrentTick(DDRMachineEntity entity) {
		return session != null ? session.currentTick(entity.level()) : 0;
	}

	private void tickSound(DDRMachineEntity entity) {
		if (session != null) {
			if (playingTrackSound == null) {
				DdrSoundInstance sound = new DdrSoundInstance(session.track(), entity, session.currentTick(entity.level()));
				Minecraft.getInstance().getSoundManager().play(sound);
				playingTrackSound = sound;
			}
			playingTrackSound.setFadeIn(shouldPlaySound(entity));
		} else {
			stopSound();
		}
	}

	private boolean shouldPlaySound(DDRMachineEntity entity) {
		Entity cameraEntity = Minecraft.getInstance().getCameraEntity();
		if (cameraEntity == null) {
			return false;
		}
		if (entity.getControllingPassenger() == cameraEntity) {
			return true;
		}
		// This is pretty hacky, but it gets the job done
		AABB bounds = cameraEntity.getBoundingBox().inflate(SOUND_RANGE);
		Optional<DDRMachineEntity> closestPlayingDdrMachine = cameraEntity.level().getEntitiesOfClass(DDRMachineEntity.class, bounds).stream()
				.filter(DDRMachineEntity::isPlayingSound)
				.min(Comparator.comparingDouble(cameraEntity::distanceToSqr));
		return closestPlayingDdrMachine.isPresent() && closestPlayingDdrMachine.get() == entity;
	}

	private void stopSound() {
		if (playingTrackSound != null) {
			Minecraft.getInstance().getSoundManager().stop(playingTrackSound);
			playingTrackSound = null;
		}
	}

	public DdrInput getCurrentInput(DDRMachineEntity entity) {
		if (entity.getControllingPassenger() instanceof LocalPlayer) {
			return lastInput;
		}
		return entity.getPlayerInput();
	}

	public DdrPlayerPoseState poseState() {
		return poseState;
	}

	public void onRemoved() {
		stopSound();
	}

	public boolean isPlayingSound() {
		return session != null;
	}

	private sealed interface Session {
		void tick(DDRMachineEntity entity);

		void handleLocalInput(DdrInput newInput, long currentTick);

		void handleRemoteInputHit(DDRMachineEntity entity, long inputTick);

		long startedAtTime();

		default long currentTick(Level level) {
			return Math.max(level.getGameTime() - startedAtTime(), 0);
		}

		Holder<JukeboxSong> track();

		Collection<TimedDdrInput> pendingInputs();
	}

	private static final class PlaySession implements Session {
		private final Holder<DdrLevel> level;
		private final DdrLevelInputQueue inputQueue;
		private final long startedAtTime;

		public PlaySession(Holder<DdrLevel> level, long startedAtTime) {
			this.level = level;
			inputQueue = new DdrLevelInputQueue(DdrServerSession.TICK_RANGE_EITHER_SIDE, level);
			this.startedAtTime = startedAtTime;
		}

		@Override
		public void tick(DDRMachineEntity entity) {
			if (!(entity.getControllingPassenger() instanceof LocalPlayer)) {
				inputQueue.discardExpiredInputs(currentTick(entity.level()));
			}
		}

		@Override
		public void handleLocalInput(DdrInput newInput, long currentTick) {
			inputQueue.handleInput(newInput, currentTick);
		}

		@Override
		public void handleRemoteInputHit(DDRMachineEntity entity, long inputTick) {
			if (!(entity.getControllingPassenger() instanceof LocalPlayer)) {
				inputQueue.clearInputAt(inputTick);
			}
		}

		@Override
		public long startedAtTime() {
			return startedAtTime;
		}

		@Override
		public Holder<JukeboxSong> track() {
			return level.value().track();
		}

		@Override
		public Collection<TimedDdrInput> pendingInputs() {
			return inputQueue.pendingInputs();
		}
	}

	private record RecordingSession(Holder<JukeboxSong> track, long startedAtTime) implements Session {
		@Override
		public void tick(DDRMachineEntity entity) {
		}

		@Override
		public void handleLocalInput(DdrInput newInput, long currentTick) {
		}

		@Override
		public void handleRemoteInputHit(DDRMachineEntity entity, long inputTick) {
		}

		@Override
		public Collection<TimedDdrInput> pendingInputs() {
			return List.of();
		}
	}
}
