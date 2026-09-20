package org.lovetropics.games.common.content.crafting_bee;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.component.ItemContainerContents;

public record CraftedUsing(
		int count,
		ItemContainerContents items
) {
	public static final Codec<CraftedUsing> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("count").forGetter(CraftedUsing::count),
			ItemContainerContents.CODEC.fieldOf("items").forGetter(CraftedUsing::items)
	).apply(i, CraftedUsing::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, CraftedUsing> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, CraftedUsing::count,
			ItemContainerContents.STREAM_CODEC, CraftedUsing::items,
			CraftedUsing::new
	);
}
