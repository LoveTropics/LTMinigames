package org.lovetropics.games.common.core.game.state.team;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.BossEvent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.scores.TeamColor;

import java.util.List;

import static com.lovetropics.lib.codec.MoreCodecs.inputOptionalFieldOf;

public record GameTeamConfig(Component name, DyeColor dyeColor, List<String> assignedRoles, int maxSize) {
	public static final MapCodec<GameTeamConfig> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ComponentSerialization.CODEC.fieldOf("name").forGetter(GameTeamConfig::name),
			inputOptionalFieldOf(DyeColor.CODEC, "dye", DyeColor.WHITE).forGetter(GameTeamConfig::dyeColor),
			inputOptionalFieldOf(Codec.STRING.listOf(), "assign_roles", List.of()).forGetter(GameTeamConfig::assignedRoles),
			inputOptionalFieldOf(Codec.INT, "max_size", Integer.MAX_VALUE).forGetter(GameTeamConfig::maxSize)
	).apply(i, GameTeamConfig::new));

	public static final Codec<GameTeamConfig> CODEC = MAP_CODEC.codec();

	public MutableComponent styledName() {
		return name.copy().withColor(textColor());
	}

	public TextColor textColor() {
		return teamColor().textColor();
	}

	public TeamColor teamColor() {
		return switch (dyeColor) {
			case WHITE -> TeamColor.WHITE;
			case ORANGE -> TeamColor.GOLD;
			case MAGENTA, PINK -> TeamColor.LIGHT_PURPLE;
			case LIGHT_BLUE -> TeamColor.AQUA;
			case YELLOW -> TeamColor.YELLOW;
			case LIME -> TeamColor.GREEN;
			case GRAY -> TeamColor.DARK_GRAY;
			case LIGHT_GRAY -> TeamColor.GRAY;
			case CYAN -> TeamColor.DARK_AQUA;
			case PURPLE -> TeamColor.DARK_PURPLE;
			case BLUE -> TeamColor.BLUE;
			case BROWN -> TeamColor.DARK_RED;
			case GREEN -> TeamColor.DARK_GREEN;
			case RED -> TeamColor.RED;
			case BLACK -> TeamColor.BLACK;
		};
	}

	public BossEvent.BossBarColor bossBarColor() {
		return switch (dyeColor) {
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
