package org.lovetropics.games.common.content.mcsr.shop;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/// Who a [ShopOffer] is applied to
public enum ShopTarget implements StringRepresentable {
	/// The player who buys it
	SELF("self"),
	/// Another participant, picked by the buyer - or a random one if they would rather not pick
	PICK("pick"),
	/// A random other participant
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
