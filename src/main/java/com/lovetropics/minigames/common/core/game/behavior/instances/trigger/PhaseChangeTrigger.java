package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressChannel;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressHolder;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressionPoint;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.context.ContextMap;

import java.util.ArrayList;
import java.util.Map;

public record PhaseChangeTrigger(ProgressChannel channel, Map<ProgressionPoint, GameActionList<Void>> phases) implements IGameBehavior {
	public static final MapCodec<PhaseChangeTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ProgressChannel.CODEC.optionalFieldOf("channel", ProgressChannel.MAIN).forGetter(PhaseChangeTrigger::channel),
			Codec.unboundedMap(ProgressionPoint.CODEC, GameActionList.VOID_CODEC).fieldOf("phases").forGetter(PhaseChangeTrigger::phases)
	).apply(i, PhaseChangeTrigger::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		ProgressHolder progression = channel.getOrThrow(game);

		for (var actions : phases.values()) {
			actions.register(game, events);
		}

		var remaining = new ArrayList<>(phases.entrySet());

		events.listen(GamePhaseEvents.TICK, () -> {
			var iterator = remaining.iterator();
			while (iterator.hasNext()) {
				var entry = iterator.next();
				if (progression.isAfter(entry.getKey())) {
					entry.getValue().apply(game, ContextMap.EMPTY);
					iterator.remove();
				}
			}
		});
	}
}
