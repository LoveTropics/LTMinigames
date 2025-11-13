package com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DdrInput;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;

import java.nio.file.Path;
import java.nio.file.Paths;

public record DdrLevel(
		Holder<JukeboxSong> track,
		ItemStack icon,
		Component displayName,
		Long2ObjectMap<DdrInput> ticks,
		DdrLevelDifficulty difficulty
) {
	public static final Codec<DdrLevel> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group(
			JukeboxSong.CODEC.fieldOf("track").forGetter(DdrLevel::track),
			ItemStack.CODEC.fieldOf("icon").forGetter(DdrLevel::icon),
			ComponentSerialization.CODEC.fieldOf("display_name").forGetter(DdrLevel::displayName),
			MoreCodecs.long2Object(DdrInput.CODEC).fieldOf("ticks").forGetter(DdrLevel::ticks),
			DdrLevelDifficulty.CODEC.fieldOf("difficulty").forGetter(DdrLevel::difficulty)
	).apply(i, DdrLevel::new));
	public static final Codec<Holder<DdrLevel>> CODEC = RegistryFileCodec.create(EscapeRace.DDR_LEVEL, DIRECT_CODEC);

	public static final StreamCodec<RegistryFriendlyByteBuf, DdrLevel> DIRECT_STREAM_CODEC = StreamCodec.composite(
			JukeboxSong.STREAM_CODEC, DdrLevel::track,
			ItemStack.STREAM_CODEC, DdrLevel::icon,
			ComponentSerialization.STREAM_CODEC, DdrLevel::displayName,
			ByteBufCodecs.map(Long2ObjectOpenHashMap::new, ByteBufCodecs.VAR_LONG, DdrInput.STREAM_CODEC), DdrLevel::ticks,
			DdrLevelDifficulty.STREAM_CODEC, DdrLevel::difficulty,
			DdrLevel::new
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, Holder<DdrLevel>> STREAM_CODEC = ByteBufCodecs.holder(EscapeRace.DDR_LEVEL, DIRECT_STREAM_CODEC);

	public static Path pathFor(ResourceLocation id) {
		return Paths.get("export", id.getNamespace(), EscapeRace.DDR_LEVEL.location().getNamespace(), EscapeRace.DDR_LEVEL.location().getPath(), id.getPath() + ".json");
	}
}
