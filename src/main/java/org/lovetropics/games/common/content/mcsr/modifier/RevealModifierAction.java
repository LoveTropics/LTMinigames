package org.lovetropics.games.common.content.mcsr.modifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;

import java.util.Optional;

/// Tells the targeted players what a modifier is, instead of its riddle - or a random one they don't know yet, if no
/// modifier is given (e.g. as a hint bought from the shop)
public record RevealModifierAction(Optional<String> modifier) implements IGameBehavior {
	public static final MapCodec<RevealModifierAction> CODEC = Codec.STRING.optionalFieldOf("modifier")
			.xmap(RevealModifierAction::new, RevealModifierAction::modifier);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		modifier.ifPresent(id -> GameModifiers.validate(game, id));
		events.applyToPlayers(game, (context, player) -> {
			GameModifiers modifiers = game.instanceState().getOrNull(GameModifiers.KEY);
			return modifiers != null && modifiers.reveal(player, modifier);
		});
	}
}
