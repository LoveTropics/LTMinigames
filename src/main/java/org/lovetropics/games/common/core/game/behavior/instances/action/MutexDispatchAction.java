package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.state.ActionMutexState;

import java.util.List;

public record MutexDispatchAction(
		List<Entry> entries
) implements IGameBehavior {
	public static final MapCodec<MutexDispatchAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Entry.CODEC.listOf().fieldOf("entries").forGetter(MutexDispatchAction::entries)
	).apply(i, MutexDispatchAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		for (Entry entry : entries) {
			entry.actions.register(game, events);
		}

		ActionMutexState mutexState = game.state().get(ActionMutexState.KEY);
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			for (Entry entry : entries) {
				if (entry.isClaimed(mutexState)) {
					return entry.actions.apply(game, context, targets);
				}
			}
			return false;
		});
	}

	public record Entry(
			List<Identifier> ifClaimed,
			GameActionList actions
	) {
		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
				ExtraCodecs.compactListCodec(Identifier.CODEC).optionalFieldOf("if_claimed", List.of()).forGetter(Entry::ifClaimed),
				GameActionList.MAP_CODEC.forGetter(Entry::actions)
		).apply(i, Entry::new));

		public boolean isClaimed(ActionMutexState mutexState) {
			for (Identifier id : ifClaimed) {
				if (!mutexState.isAcquiredGlobal(id)) {
					return false;
				}
			}
			return true;
		}
	}
}
