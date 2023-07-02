package com.lovetropics.minigames.common.core.integration.game_actions;

import com.google.common.base.Strings;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.Optional;
import java.util.UUID;

public record GamePackage(String packageType, String sendingPlayerName, Optional<UUID> receivingPlayer) {
	public static final MapCodec<GamePackage> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("package_type").forGetter(c -> c.packageType),
			Codec.STRING.optionalFieldOf("sending_player_name", "").forGetter(c -> c.sendingPlayerName),
			UUIDUtil.STRING_CODEC.optionalFieldOf("receiving_player").forGetter(c -> c.receivingPlayer)
	).apply(i, GamePackage::new));

	@Override
	public String sendingPlayerName() {
		return !Strings.isNullOrEmpty(sendingPlayerName) ? sendingPlayerName : null;
	}
}
