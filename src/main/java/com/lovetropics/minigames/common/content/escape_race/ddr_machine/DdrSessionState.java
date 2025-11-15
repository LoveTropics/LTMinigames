package com.lovetropics.minigames.common.content.escape_race.ddr_machine;

import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DdrLevel;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.JukeboxSong;

import java.util.Optional;

public record DdrSessionState(
		Optional<Holder<DdrLevel>> playingLevel,
		Optional<Holder<JukeboxSong>> recordingTrack,
		long startedAtTime
) {
	public static final StreamCodec<RegistryFriendlyByteBuf, DdrSessionState> STREAM_CODEC = StreamCodec.composite(
			DdrLevel.STREAM_CODEC.apply(ByteBufCodecs::optional), DdrSessionState::playingLevel,
			JukeboxSong.STREAM_CODEC.apply(ByteBufCodecs::optional), DdrSessionState::recordingTrack,
			ByteBufCodecs.VAR_LONG, DdrSessionState::startedAtTime,
			DdrSessionState::new
	);

	public static final DdrSessionState INACTIVE = new DdrSessionState(Optional.empty(), Optional.empty(), 0);

	public static DdrSessionState playing(Holder<DdrLevel> level, long startedAtTime) {
		return new DdrSessionState(Optional.of(level), Optional.empty(), startedAtTime);
	}

	public static DdrSessionState recording(Holder<JukeboxSong> track, long startedAtTime) {
		return new DdrSessionState(Optional.empty(), Optional.of(track), startedAtTime);
	}
}
