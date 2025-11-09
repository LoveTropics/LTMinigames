package com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DdrInput;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxSong;

import java.nio.file.Path;
import java.nio.file.Paths;

public record DDRMachineLevel(
		ResourceLocation id,
		ResourceKey<JukeboxSong> track,
		String name,
		Component displayName,
		Long2ObjectMap<DdrInput> ticks
) {
	public static Codec<DDRMachineLevel> codec(ResourceLocation id) {
		return RecordCodecBuilder.create(i -> i.group(
				ResourceKey.codec(Registries.JUKEBOX_SONG).fieldOf("track").forGetter(DDRMachineLevel::track),
				Codec.STRING.fieldOf("name").forGetter(DDRMachineLevel::name),
				ComponentSerialization.CODEC.fieldOf("display_name").forGetter(DDRMachineLevel::displayName),
				MoreCodecs.long2Object(DdrInput.CODEC).fieldOf("ticks").forGetter(DDRMachineLevel::ticks)
		).apply(i, (track, name, displayName, ticks) ->
				new  DDRMachineLevel(id, track, name, displayName, ticks)));
	}

	public static Path pathFor(ResourceLocation id) {
		return Paths.get("export", id.getNamespace(), "ddr_levels", id.getPath() + ".json");
	}

	public DDRMachineLevelClient toClient(){
		return new DDRMachineLevelClient(
				id,
				new ItemStack(Items.MUSIC_DISC_PIGSTEP),
				displayName
		);
	}
}
