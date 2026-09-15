package com.lovetropics.minigames.common.core.game;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/// Represents the type of donations that donors can contribute to affect the game.
/// Surfaced on the homepage where players can donate.
public enum GameDonationType implements StringRepresentable {
	/// This is our "typical" type - where players can choose between different packages to affect the game or a particular player
	PACKAGES("packages"),
	/// This only gives donors the option to contribute directly to specific players
	PARTICIPANT_CONTRIBUTIONS("participant_contributions");

	private final String id;

	GameDonationType(final String id) {
		this.id = id;
	}

	public static final Codec<GameDonationType> CODEC = StringRepresentable.fromEnum(GameDonationType::values);

	@Override
	public String getSerializedName() {
		return id;
	}
}
