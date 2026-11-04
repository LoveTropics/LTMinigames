package org.lovetropics.games.common.content.shop;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum ShopUsers implements StringRepresentable {
	PLAYERS("players"),
	GHOSTS("ghosts"),
	OVERLORDS("overlords"),
	ALL("all"),
	;

	public static final Codec<ShopUsers> CODEC = StringRepresentable.fromEnum(ShopUsers::values);

	private final String name;

	ShopUsers(String name) {
		this.name = name;
	}

	public boolean includesPlayers() {
		return switch (this) {
			case PLAYERS, ALL -> true;
			default -> false;
		};
	}

	public boolean includesGhosts() {
		return switch (this) {
			case GHOSTS, ALL -> true;
			default -> false;
		};
	}

	public boolean includesOverlords() {
		return switch (this) {
			case OVERLORDS, ALL -> true;
			default -> false;
		};
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
