package com.lovetropics.minigames.common.core.game.behavior.action;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventListeners;
import com.mojang.serialization.Codec;
import org.apache.commons.lang3.function.ToBooleanBiFunction;

import java.util.List;

public record NoneActionTarget() implements ActionTarget<Void> {
	public static final NoneActionTarget INSTANCE = new NoneActionTarget();
	public static final Codec<NoneActionTarget> CODEC = Codec.unit(INSTANCE);

	@Override
	public List<Void> resolve(IGamePhase phase, Iterable<Void> sources) {
		return List.of();
	}

	@Override
	public boolean apply(IGamePhase game, GameEventListeners listeners, GameActionContext context, Iterable<Void> sources) {
		return false;
	}

	@Override
	public void listenAndCaptureSource(EventRegistrar listeners, ToBooleanBiFunction<GameActionContext, Iterable<Void>> listener) {
		listeners.listen(GameActionEvents.APPLY, context -> listener.applyAsBoolean(context, List.of()));
	}

	@Override
	public Codec<NoneActionTarget> type() {
		return ActionTargetTypes.NONE.get();
	}
}
