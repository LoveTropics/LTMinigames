package com.lovetropics.minigames.common.core.integration.game_actions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Util;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

import java.util.List;
import java.util.UUID;

public record Donation(String name, double amount, String comments, boolean anonymous, double total, String minecraftName, UUID minecraftUuid) {
	public static final MapCodec<Donation> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("name").forGetter(Donation::name),
			Codec.DOUBLE.fieldOf("amount").forGetter(Donation::amount),
			Codec.STRING.optionalFieldOf("comments", "").forGetter(Donation::comments),
			Codec.BOOL.optionalFieldOf("anonymous", false).forGetter(Donation::anonymous),
			Codec.DOUBLE.fieldOf("total").forGetter(Donation::total),
			Codec.STRING.optionalFieldOf("minecraft_name", "").forGetter(Donation::minecraftName),
			UUIDUtil.AUTHLIB_CODEC.lenientOptionalFieldOf("minecraft_uuid", Util.NIL_UUID).forGetter(Donation::minecraftUuid)
	).apply(i, Donation::new));

	public static final Codec<Donation> CODEC = MAP_CODEC.codec();
	public static final Codec<List<Donation>> LIST_CODEC = Donation.CODEC.listOf();

	private static final List<String> ANONYMOUS_NAMES = List.of(
			"Anonymous Andy",
			"Secretive Sally",
			"Timid Tommy"
	);

	public Component getDisplayName(int color, RandomSource random) {
		final String name = anonymous() ? Util.getRandom(ANONYMOUS_NAMES, random) : minecraftName().isEmpty() ? name() : minecraftName();
		return Component.literal(name).withColor(color);
	}

	public static Donation empty() {
		return new Donation(
				"",
				0.0,
				"",
				false,
				0.0,
				"",
				Util.NIL_UUID
		);
	}
}
