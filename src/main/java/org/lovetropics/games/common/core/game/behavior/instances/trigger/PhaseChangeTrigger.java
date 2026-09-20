package org.lovetropics.games.common.core.game.behavior.instances.trigger;

import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.state.progress.ProgressChannel;
import org.lovetropics.games.common.core.game.state.progress.ProgressHolder;
import org.lovetropics.games.common.core.game.state.progress.ProgressionPoint;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.context.ContextMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public record PhaseChangeTrigger(ProgressChannel channel, Map<ProgressionPoint, GameActionList> phases) implements IGameBehavior {
	public static final MapCodec<PhaseChangeTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ProgressChannel.CODEC.optionalFieldOf("channel", ProgressChannel.MAIN).forGetter(PhaseChangeTrigger::channel),
			Codec.unboundedMap(ProgressionPoint.CODEC, GameActionList.CODEC).fieldOf("phases").forGetter(PhaseChangeTrigger::phases)
	).apply(i, PhaseChangeTrigger::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		ProgressHolder progression = channel.getOrThrow(game);

		for (GameActionList actions : phases.values()) {
			actions.register(game, events);
		}

		ArrayList<Map.Entry<ProgressionPoint, GameActionList>> remaining = new ArrayList<>(phases.entrySet());

		events.listen(GamePhaseEvents.TICK, () -> {
			Iterator<Map.Entry<ProgressionPoint, GameActionList>> iterator = remaining.iterator();
			while (iterator.hasNext()) {
				Map.Entry<ProgressionPoint, GameActionList> entry = iterator.next();
				if (progression.isAfter(entry.getKey())) {
					entry.getValue().apply(game, ContextMap.EMPTY);
					iterator.remove();
				}
			}
		});
	}
}
