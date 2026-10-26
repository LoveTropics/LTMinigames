package org.lovetropics.games.common.core.integration.game_actions;

import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;

import java.util.Optional;

/// @param specificTargets Requested targets from the donor - may include players within a sub-game, must be filtered before passing down.
public record GamePackage(
		String packageType,
		@Nullable String sendingPlayerName,
		@Nullable ActionSubjects<?> specificTargets,
		Optional<Double> amount
) {
}
