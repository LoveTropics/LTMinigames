package com.lovetropics.minigames.common.core.game.behavior.instances.trigger.phase;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.context.ContextMap;

import java.util.Optional;
import java.util.function.Supplier;

public record StopGameTrigger(GameActionList<Void> actions, Optional<GameActionList<Void>> finish, Optional<GameActionList<Void>> cancel) implements IGameBehavior {
	public static final MapCodec<StopGameTrigger> CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
			GameActionList.VOID_CODEC.fieldOf("actions").forGetter(StopGameTrigger::actions),
			GameActionList.VOID_CODEC.optionalFieldOf("finish").forGetter(StopGameTrigger::finish),
			GameActionList.VOID_CODEC.optionalFieldOf("cancel").forGetter(StopGameTrigger::cancel)
	).apply(in, StopGameTrigger::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		actions.register(game, events);
		finish.ifPresent(f -> f.register(game, events));
		cancel.ifPresent(c -> c.register(game, events));

		events.listen(GamePhaseEvents.STOP, reason -> {
			actions.apply(game, ContextMap.EMPTY);
			if (reason.isFinished()) {
				finish.ifPresent(f -> f.apply(game, ContextMap.EMPTY));
			} else {
				cancel.ifPresent(c -> c.apply(game, ContextMap.EMPTY));
			}
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.STOP_GAME;
	}
}
