package org.lovetropics.games.common.content.shop;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum ShopUsers implements StringRepresentable {
	PLAYERS("players"),
	GHOSTS("ghosts"),
	ALL("all"),
	;

	public static final Codec<ShopUsers> CODEC = StringRepresentable.fromEnum(ShopUsers::values);

	private final String name;

	ShopUsers(String name) {
		this.name = name;
	}

	public boolean includesPlayers() {
		return this != GHOSTS;
	}

	public boolean includesGhosts() {
		return this != PLAYERS;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
