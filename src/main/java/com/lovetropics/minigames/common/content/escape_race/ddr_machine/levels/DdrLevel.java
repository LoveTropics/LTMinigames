package com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DdrInput;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.JukeboxSong;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

public record DdrLevel(
		Holder<JukeboxSong> track,
		ItemStackTemplate icon,
		Component displayName,
		List<TimedDdrInput> inputs,
		DdrLevelDifficulty difficulty
) {
	private static final Codec<List<TimedDdrInput>> INPUTS_CODEC = MoreCodecs.long2Object(DdrInput.CODEC).xmap(
			map -> map.long2ObjectEntrySet().stream()
					.map(entry -> new TimedDdrInput(entry.getLongKey(), entry.getValue()))
					.sorted(Comparator.comparingLong(TimedDdrInput::tick))
					.toList(),
			inputs -> {
				Long2ObjectMap<DdrInput> map = new Long2ObjectLinkedOpenHashMap<>();
				for (TimedDdrInput input : inputs) {
					map.put(input.tick(), input.input());
				}
				return map;
			}
	);

	public static final Codec<DdrLevel> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group(
			JukeboxSong.CODEC.fieldOf("track").forGetter(DdrLevel::track),
			ItemStackTemplate.CODEC.fieldOf("icon").forGetter(DdrLevel::icon),
			ComponentSerialization.CODEC.fieldOf("display_name").forGetter(DdrLevel::displayName),
			INPUTS_CODEC.fieldOf("ticks").forGetter(DdrLevel::inputs),
			DdrLevelDifficulty.CODEC.fieldOf("difficulty").forGetter(DdrLevel::difficulty)
	).apply(i, DdrLevel::new));
	public static final Codec<Holder<DdrLevel>> CODEC = RegistryFileCodec.create(EscapeRace.DDR_LEVEL, DIRECT_CODEC);

	public static final StreamCodec<RegistryFriendlyByteBuf, DdrLevel> DIRECT_STREAM_CODEC = StreamCodec.composite(
			JukeboxSong.STREAM_CODEC, DdrLevel::track,
			ItemStackTemplate.STREAM_CODEC, DdrLevel::icon,
			ComponentSerialization.STREAM_CODEC, DdrLevel::displayName,
			TimedDdrInput.STREAM_CODEC.apply(ByteBufCodecs.list()), DdrLevel::inputs,
			DdrLevelDifficulty.STREAM_CODEC, DdrLevel::difficulty,
			DdrLevel::new
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, Holder<DdrLevel>> STREAM_CODEC = ByteBufCodecs.holder(EscapeRace.DDR_LEVEL, DIRECT_STREAM_CODEC);

	public int lengthInTicks() {
		return track.value().lengthInTicks();
	}

	public static Path pathFor(Identifier id) {
		return Paths.get("export", id.getNamespace(), EscapeRace.DDR_LEVEL.identifier().getNamespace(), EscapeRace.DDR_LEVEL.identifier().getPath(), id.getPath() + ".json");
	}
}
