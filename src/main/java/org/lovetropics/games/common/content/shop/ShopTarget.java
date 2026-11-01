package org.lovetropics.games.common.content.shop;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum ShopTarget implements StringRepresentable {
	SELF("self"),
	PICK("pick"),
	RANDOM("random"),
	;

	public static final Codec<ShopTarget> CODEC = StringRepresentable.fromEnum(ShopTarget::values);

	private final String name;

	ShopTarget(String name) {
		this.name = name;
	}

	public boolean isOtherPlayer() {
		return this != SELF;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
