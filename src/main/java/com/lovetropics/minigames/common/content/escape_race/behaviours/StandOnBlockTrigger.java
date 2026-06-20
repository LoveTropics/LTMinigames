package com.lovetropics.minigames.common.content.escape_race.behaviours;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.advancements.predicates.BlockPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.context.ContextMap;

import java.util.Set;
import java.util.UUID;

public record StandOnBlockTrigger(BlockPredicate predicate, GameActionList apply, GameActionList tick, GameActionList clear, int tickFrequency) implements IGameBehavior {

	public static final MapCodec<StandOnBlockTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BlockPredicate.CODEC.fieldOf("predicate").forGetter(StandOnBlockTrigger::predicate),
			GameActionList.CODEC.fieldOf("apply").forGetter(StandOnBlockTrigger::apply),
			GameActionList.CODEC.fieldOf("tick").forGetter(StandOnBlockTrigger::tick),
			GameActionList.CODEC.fieldOf("clear").forGetter(StandOnBlockTrigger::clear),
			Codec.INT.optionalFieldOf("tick_frequency", 20).forGetter(StandOnBlockTrigger::tickFrequency)
	).apply(i, StandOnBlockTrigger::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		apply.register(game, events);
		clear.register(game, events);
		tick.register(game, events);

		final Set<UUID> appliedToPlayers = new ObjectOpenHashSet<>();
		events.listen(GamePlayerEvents.TICK, player -> {
			final BlockPos standingOn = player.getOnPos();
			if (predicate.matches(player.level(), standingOn)) {
				if (appliedToPlayers.add(player.getUUID())) {
					apply.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player));
				}
				if (player.tickCount % tickFrequency == 0) {
					tick.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player));
				}
			} else {
				if (appliedToPlayers.remove(player.getUUID())) {
					clear.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player));
				}
			}
		});
		events.listen(GamePlayerEvents.REMOVE, player -> appliedToPlayers.remove(player.getUUID()));
	}
}
