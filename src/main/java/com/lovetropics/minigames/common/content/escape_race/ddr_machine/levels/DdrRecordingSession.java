package com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels;

import com.google.gson.JsonElement;
import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DdrInput;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxSong;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DdrRecordingSession {
	private static final Logger LOGGER = LogUtils.getLogger();

	private final Component displayName;
	private final Holder<JukeboxSong> track;

	private final long startedAtTime;
	private final List<TimedDdrInput> inputs = new ArrayList<>();

	private boolean stopped;

	public DdrRecordingSession(Component displayName, Holder<JukeboxSong> track, long startedAtTime) {
		this.displayName = displayName;
		this.track = track;
		this.startedAtTime = startedAtTime;
	}

	public void handleInput(DdrInput newInput, long inputTick) {
		if (newInput.isEmpty() || inputTick >= track.value().lengthInTicks()) {
			return;
		}
		inputs.add(new TimedDdrInput(inputTick, newInput));
	}

	@Nullable
	public Recording tick(ServerPlayer player) {
		if (stopped) {
			return null;
		}
		long currentTick = player.level().getGameTime() - startedAtTime;
		if (currentTick >= track.value().lengthInTicks()) {
			return stopRecording();
		}
		return null;
	}

	public Recording stopRecording() {
		stopped = true;
		return new Recording(new DdrLevel(
				track,
				new ItemStack(Items.MUSIC_DISC_PIGSTEP),
				displayName,
				List.copyOf(inputs),
				DdrLevelDifficulty.EASY
		));
	}

	public record Recording(DdrLevel level) {
		public void export(HolderLookup.Provider registries) {
			Util.ioPool().execute(() -> {
				RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
				JsonElement output = DdrLevel.DIRECT_CODEC.encodeStart(ops, level).getOrThrow();
				try {
					ResourceLocation location = LoveTropics.location(level.track().unwrapKey().orElseThrow().location().getPath());
					Path path = DdrLevel.pathFor(location);
					Files.createDirectories(path.getParent());
					Files.deleteIfExists(path);
					Files.writeString(path, output.toString());
				} catch (IOException e) {
					LOGGER.error("Failed to export recording", e);
				}
			});
		}
	}
}
