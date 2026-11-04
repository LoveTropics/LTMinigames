package org.lovetropics.games.common.content.shop;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum ShopOfferFlavor implements StringRepresentable {
	BONUS("bonus"),
	SABOTAGE("sabotage"),
	;

	public static final Codec<ShopOfferFlavor> CODEC = StringRepresentable.fromEnum(ShopOfferFlavor::values);

	private final String name;

	ShopOfferFlavor(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
