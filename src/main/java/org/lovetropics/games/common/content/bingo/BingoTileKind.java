package org.lovetropics.games.common.content.bingo;

import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import org.lovetropics.games.LoveTropics;

// fancy textures by brick!
public enum BingoTileKind implements StringRepresentable {
	OBTAIN("obtain"),
	CRAFT("craft"),
	CHALLENGE("challenge"),
	;

	public static final StringRepresentable.EnumCodec<BingoTileKind> CODEC = StringRepresentable.fromEnum(BingoTileKind::values);

	private final String name;
	private final Identifier sprite;

	BingoTileKind(String name) {
		this.name = name;
		sprite = Identifier.fromNamespaceAndPath(LoveTropics.ID, "minigames/tropic_rush/" + name + "_tile");
	}

	public Identifier sprite() {
		return sprite;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
