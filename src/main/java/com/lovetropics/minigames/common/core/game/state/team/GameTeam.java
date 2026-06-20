package com.lovetropics.minigames.common.core.game.state.team;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.scores.TeamColor;

public record GameTeam(GameTeamKey key, GameTeamConfig config) {
	public static final Codec<GameTeam> CODEC = RecordCodecBuilder.create(i -> i.group(
			GameTeamKey.CODEC.fieldOf("key").forGetter(GameTeam::key),
			GameTeamConfig.MAP_CODEC.forGetter(GameTeam::config)
	).apply(i, GameTeam::new));

	public Payload asPayload() {
		return new Payload(key.id(), config.name(), config.name().getString(), config.teamColor());
	}

	public record Payload(
			String id,
			Component name,
			String englishName,
			TeamColor color
	) {
		public static final Codec<Payload> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("id").forGetter(Payload::id),
				ComponentSerialization.CODEC.fieldOf("name").forGetter(Payload::name),
				Codec.STRING.fieldOf("english_name").forGetter(Payload::englishName),
				TeamColor.CODEC.fieldOf("color").forGetter(Payload::color)
		).apply(i, Payload::new));
	}
}
