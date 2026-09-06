package com.lovetropics.minigames.common.core.game;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import java.util.Optional;

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

	default Identifier backendId() {
		return id();
	}

	/// An identifier for telemetry usage, so that variants of games can share
	/// statistics. Defaults to the ID if not set in the JSON.
	///
	/// @return The statistics key for this minigame.
	default String statisticsKey() {
		return id().getPath();
	}

	Component name();

	@Nullable
	default Component subtitle() {
		return null;
	}

	@Nullable
	default Identifier icon() {
		return null;
	}

	@Nullable
	default Identifier introSlideshow() {
		return null;
	}

	/// Will only select up to this many participants to actually play
	/// in the started minigame. The rest of the players registered for
	/// the minigame will be slotted in as spectators where they can watch
	/// the minigame unfold.
	///
	/// @return The maximum amount of players that can be participants in the
	/// minigame.
	default int getMaximumParticipantCount() {
		return Integer.MAX_VALUE;
	}

	default boolean hideFromList() {
		return false;
	}

	IGamePhaseDefinition getPlayingPhase();

	default Optional<IGamePhaseDefinition> getWaitingPhase() {
		return Optional.empty();
	}
}
