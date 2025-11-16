package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContextKeys;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;

import java.util.Map;

public record WhileInRegionTrigger(Map<String, GameActionList> regionActions, int interval) implements IGameBehavior {
	public static final MapCodec<WhileInRegionTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.unboundedMap(Codec.STRING, GameActionList.CODEC).fieldOf("regions").forGetter(WhileInRegionTrigger::regionActions),
			Codec.INT.optionalFieldOf("interval", 20).forGetter(WhileInRegionTrigger::interval)
	).apply(i, WhileInRegionTrigger::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		for (GameActionList actions : regionActions.values()) {
			actions.register(game, events);
		}

		events.listen(GamePlayerEvents.TICK, player -> {
			if (player.tickCount % interval != 0) {
				return;
			}

			for (var entry : regionActions.entrySet()) {
				if (isPlayerInRegion(game, player, entry.getKey())) {
					GameActionList actions = entry.getValue();
					ContextMap context = new ContextMap.Builder()
							.withParameter(GameActionContextKeys.NAME, player.getDisplayName())
							.create(ContextKeySet.EMPTY);
					actions.apply(game, context, ActionSubjects.ofPlayer(player));
				}
			}
		});
	}

	private boolean isPlayerInRegion(IGamePhase game, ServerPlayer player, String key) {
		for (BlockBox region : game.mapRegions().get(key)) {
			if (region.contains(player.getX(), player.getY(), player.getZ())) {
				return true;
			}
		}
		return false;
	}
}
