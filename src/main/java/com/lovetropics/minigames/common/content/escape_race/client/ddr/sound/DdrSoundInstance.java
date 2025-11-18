package com.lovetropics.minigames.common.content.escape_race.client.ddr.sound;

import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DDRMachineEntity;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.JukeboxSong;

import javax.annotation.Nullable;
import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class DdrSoundInstance extends AbstractTickableSoundInstance {
	private static final float VOLUME = 4.0f;
	public static final float FADE_SPEED = 0.1f;

	private final DDRMachineEntity entity;
	private final long startTick;
	private boolean fadeIn;

	public DdrSoundInstance(Holder<JukeboxSong> track, DDRMachineEntity entity, long startTick) {
		super(track.value().soundEvent().value(), SoundSource.RECORDS, SoundInstance.createUnseededRandom());
		this.entity = entity;
		this.startTick = startTick;
		volume = 0.0f;
		x = (float) entity.getX();
		y = (float) entity.getY();
		z = (float) entity.getZ();
	}

	@Override
	public boolean canStartSilent() {
		return true;
	}

	@Override
	public void tick() {
		if (entity.isRemoved()) {
			stop();
			return;
		}
		x = (float) entity.getX();
		y = (float) entity.getY();
		z = (float) entity.getZ();
		if (fadeIn) {
			volume = Math.min(volume + FADE_SPEED, VOLUME);
		} else {
			volume = Math.max(volume - FADE_SPEED, 0.0f);
		}
	}

	public void setFadeIn(boolean fadeIn) {
		this.fadeIn = fadeIn;
	}

	@Override
	public CompletableFuture<AudioStream> getStream(SoundBufferLibrary soundBuffers, Sound sound, boolean looping) {
		return super.getStream(soundBuffers, sound, looping).thenApplyAsync(stream -> {
			try {
				return clipStream(stream, startTick);
			} catch (IOException e) {
				throw new CompletionException(e);
			}
		}, Util.nonCriticalIoPool());
	}

	private static AudioStream clipStream(AudioStream stream, long startTick) throws IOException {
		AudioFormat format = stream.getFormat();
		float seconds = (float) startTick / SharedConstants.TICKS_PER_SECOND;
		int samples = Mth.floor(seconds * format.getSampleRate());
		int bytes = (samples * format.getSampleSizeInBits()) / Byte.SIZE;
		while (bytes > 0) {
			ByteBuffer buffer = stream.read(bytes);
			if (buffer.remaining() == 0) {
				// Reached EOF, it will just be an empty stream anyway
				return stream;
			} else if (bytes < buffer.remaining()) {
				// We read back more bits than we asked for, clip out just what we want to skip and then resume
				return new PrependedStream(buffer.position(bytes), stream);
			}
			bytes -= buffer.remaining();
		}
		return stream;
	}

	private static class PrependedStream implements AudioStream {
		@Nullable
		private ByteBuffer precedingBuffer;
		private final AudioStream stream;

		private PrependedStream(ByteBuffer precedingBuffer, AudioStream stream) {
			this.precedingBuffer = precedingBuffer;
			this.stream = stream;
		}

		@Override
		public AudioFormat getFormat() {
			return stream.getFormat();
		}

		@Override
		public ByteBuffer read(int size) throws IOException {
			ByteBuffer precedingBuffer = this.precedingBuffer;
			if (precedingBuffer != null) {
				this.precedingBuffer = null;
				return precedingBuffer;
			}
			return stream.read(size);
		}

		@Override
		public void close() throws IOException {
			precedingBuffer = null;
			stream.close();
		}
	}
}
