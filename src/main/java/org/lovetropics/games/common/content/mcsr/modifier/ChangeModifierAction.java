package org.lovetropics.games.common.content.mcsr.modifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.context.ContextMap;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.GameActionContextKeys;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.integration.game_actions.GamePackage;

import java.util.Optional;

/// Makes a modifier stronger (or weaker, with a negative amount) for everyone, no matter who the action targets
///
/// @param perDonatedAmount if set, the amount is instead the donated amount of the package times this.
///                         Packages without an amount, such as those sent by command, use the amount.
public record ChangeModifierAction(String modifier, int amount, Optional<Double> perDonatedAmount) implements IGameBehavior {
	public static final MapCodec<ChangeModifierAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("modifier").forGetter(ChangeModifierAction::modifier),
			Codec.INT.optionalFieldOf("amount", 1).forGetter(ChangeModifierAction::amount),
			Codec.DOUBLE.optionalFieldOf("per_donated_amount").forGetter(ChangeModifierAction::perDonatedAmount)
	).apply(i, ChangeModifierAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		GameModifiers.validate(game, modifier);
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			GameModifiers modifiers = game.instanceState().getOrNull(GameModifiers.KEY);
			return modifiers != null && modifiers.change(modifier, getAmount(context));
		});
	}

	private int getAmount(ContextMap context) {
		Optional<Double> donatedAmount = Optional.ofNullable(context.getOptional(GameActionContextKeys.PACKAGE)).flatMap(GamePackage::amount);
		if (perDonatedAmount.isPresent() && donatedAmount.isPresent()) {
			return (int) Math.floor(donatedAmount.get() * perDonatedAmount.get());
		}
		return amount;
	}
}
