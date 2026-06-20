package com.lovetropics.minigames.common.core.game.state.team;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.scores.TeamColor;

import java.util.List;

import static com.lovetropics.lib.codec.MoreCodecs.inputOptionalFieldOf;

public record GameTeamConfig(Component name, DyeColor dye, TeamColor teamColor, List<String> assignedRoles, int maxSize) {
	public static final MapCodec<GameTeamConfig> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ComponentSerialization.CODEC.fieldOf("name").forGetter(GameTeamConfig::name),
			inputOptionalFieldOf(DyeColor.CODEC, "dye", DyeColor.WHITE).forGetter(GameTeamConfig::dye),
			inputOptionalFieldOf(TeamColor.CODEC, "text", TeamColor.WHITE).forGetter(GameTeamConfig::teamColor),
			inputOptionalFieldOf(Codec.STRING.listOf(), "assign_roles", List.of()).forGetter(GameTeamConfig::assignedRoles),
			inputOptionalFieldOf(Codec.INT, "max_size", Integer.MAX_VALUE).forGetter(GameTeamConfig::maxSize)
	).apply(i, GameTeamConfig::new));

	public static final Codec<GameTeamConfig> CODEC = MAP_CODEC.codec();

	public MutableComponent styledName() {
		return name.copy().withColor(teamColor.textColor());
	}

	public BossEvent.BossBarColor bossBarColor() {
		return switch (dye) {
			case WHITE, GRAY, LIGHT_GRAY -> BossEvent.BossBarColor.WHITE;
			case ORANGE, RED -> BossEvent.BossBarColor.RED;
			case MAGENTA, BLACK, PURPLE -> BossEvent.BossBarColor.PURPLE;
			case LIGHT_BLUE, BLUE, CYAN -> BossEvent.BossBarColor.BLUE;
			case YELLOW, BROWN -> BossEvent.BossBarColor.YELLOW;
			case LIME, GREEN -> BossEvent.BossBarColor.GREEN;
			case PINK -> BossEvent.BossBarColor.PINK;
		};
	}
}
