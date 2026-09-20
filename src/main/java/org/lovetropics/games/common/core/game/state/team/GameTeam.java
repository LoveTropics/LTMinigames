package org.lovetropics.games.common.core.game.state.team;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.BossEvent;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.scores.TeamColor;

public record GameTeam(
		GameTeamKey key,
		DyeColor dyeColor,
		Component plainName
) {
	public MutableComponent styledName() {
		return plainName.copy().withColor(textColor());
	}

	public TextColor textColor() {
		return teamColor().textColor();
	}

	public TeamColor teamColor() {
		return teamColor(dyeColor);
	}

	public static TeamColor teamColor(DyeColor dyeColor) {
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

	public Block glassBlock() {
		return Blocks.STAINED_GLASS.pick(dyeColor);
	}

	public Payload asPayload() {
		return new Payload(key.id(), plainName, plainName.getString(), teamColor());
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
