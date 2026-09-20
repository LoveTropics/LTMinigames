package com.lovetropics.minigames.common.core.game;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/// Used as a discriminant for a registered minigame. Defines the logic of the
/// minigame as it is actively running, and provides methods to customize the
/// ruleset for the minigame such as maximum and minimum participants, game types
/// for each player type, dimension the minigame takes place in, etc.
public interface IGameDefinition {
	/// The identifier for this minigame definition. Must be unique
	/// compared to other registered minigames.
	///
	/// @return The identifier for this minigame definition.
	Identifier id();

	Component name();

	default @Nullable Component subtitle() {
		return null;
	}

	/// Will only select up to this many participants to actually play
	/// in the started minigame. The rest of the players registered for
	/// the minigame will be slotted in as spectators where they can watch
	/// the minigame unfold.
	///
	/// @return The maximum amount of players that can be participants in the
	/// minigame.
	default int maximumParticipants() {
		return Integer.MAX_VALUE;
	}
}
