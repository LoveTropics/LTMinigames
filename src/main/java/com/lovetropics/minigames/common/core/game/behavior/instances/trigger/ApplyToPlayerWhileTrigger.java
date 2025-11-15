package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextMap;

import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public record ApplyToPlayerWhileTrigger(EntityPredicate predicate, GameActionList<ServerPlayer> apply, GameActionList<ServerPlayer> clear) implements IGameBehavior {
	public static final MapCodec<ApplyToPlayerWhileTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			EntityPredicate.CODEC.fieldOf("predicate").forGetter(ApplyToPlayerWhileTrigger::predicate),
			GameActionList.PLAYER_CODEC.fieldOf("apply").forGetter(ApplyToPlayerWhileTrigger::apply),
			GameActionList.PLAYER_CODEC.fieldOf("clear").forGetter(ApplyToPlayerWhileTrigger::clear)
	).apply(i, ApplyToPlayerWhileTrigger::new));

	@Override
	public void register(final IGamePhase game, final EventRegistrar events) {
		apply.register(game, events);
		clear.register(game, events);

		final Set<UUID> appliedToPlayers = new ObjectOpenHashSet<>();
		events.listen(GamePlayerEvents.TICK, player -> {
			if (predicate.matches(player, player)) {
				if (appliedToPlayers.add(player.getUUID())) {
					apply.apply(game, ContextMap.EMPTY, player);
				}
			} else {
				if (appliedToPlayers.remove(player.getUUID())) {
					clear.apply(game, ContextMap.EMPTY, player);
				}
			}
		});
		events.listen(GamePlayerEvents.REMOVE, player -> appliedToPlayers.remove(player.getUUID()));
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.APPLY_TO_PLAYER_WHILE;
	}
}
