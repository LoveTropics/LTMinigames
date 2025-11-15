package com.lovetropics.minigames.common.content.escape_race.client.ddr;

import com.lovetropics.minigames.common.content.escape_race.client.ddr.sound.DdrSoundInstance;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DDRMachineEntity;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DdrInput;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DdrLevel;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DdrLevelInputQueue;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DdrServerSession;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.TimedDdrInput;
import com.lovetropics.minigames.common.core.network.ddr.ServerboundDdrInputPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public class ClientDdrMachine {
	@Nullable
	private Session session;

	private DdrInput lastInput = DdrInput.NONE;

	private final DdrPlayerPoseState poseState = new DdrPlayerPoseState();
	@Nullable
	private SoundInstance playingTrackSound;

	public void tick(DDRMachineEntity entity) {
		if (entity.getControllingPassenger() instanceof LocalPlayer player) {
			tickControlledLocal(entity, player);
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

	private void tickControlledLocal(DDRMachineEntity entity, LocalPlayer player) {
		DdrInput input = DdrInput.fromKeyPresses(player.input.keyPresses);
		DdrInput newInput = input.subtract(lastInput);

		long currentTick = getCurrentTick(entity);
		if (!input.equals(lastInput)) {
			ClientPacketDistributor.sendToServer(new ServerboundDdrInputPacket(input, currentTick));
			lastInput = input;
		}

		if (session != null) {
			session.handleLocalInput(newInput, currentTick);
		}
	}

	public void handleRemoteInputHit(DDRMachineEntity entity, long inputTick) {
		if (session != null) {
			session.handleRemoteInputHit(entity, inputTick);
		}
	}

	public void startPlaying(Holder<DdrLevel> level, long startedAtTime) {
		session = new PlaySession(level, startedAtTime);
	}

	public void startRecording(Holder<JukeboxSong> track, long startedAtTime) {
		session = new RecordingSession(track, startedAtTime);
	}

	public void clearSession() {
		session = null;
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
				SoundInstance sound = new DdrSoundInstance(session.track(), entity, session.currentTick(entity.level()));
				Minecraft.getInstance().getSoundManager().play(sound);
				playingTrackSound = sound;
			}
		} else {
			stopSound();
		}
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
