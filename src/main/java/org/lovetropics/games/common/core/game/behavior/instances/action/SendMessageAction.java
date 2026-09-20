package org.lovetropics.games.common.core.game.behavior.instances.action;

import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorTypes;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.util.TemplatedText;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.function.Supplier;

public record SendMessageAction(TemplatedText message, boolean actionBar) implements IGameBehavior {
	public static final MapCodec<SendMessageAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			TemplatedText.CODEC.fieldOf("message").forGetter(SendMessageAction::message),
			Codec.BOOL.optionalFieldOf("action_bar", false).forGetter(SendMessageAction::actionBar)
	).apply(i, SendMessageAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.applyToPlayers(game, (context, target) -> {
			target.sendSystemMessage(message.apply(context), actionBar);
			return true;
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.SEND_MESSAGE;
	}
}
