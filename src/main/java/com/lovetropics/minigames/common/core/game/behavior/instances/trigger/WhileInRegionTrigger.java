package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.BbMobSpawner;
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
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record WhileInRegionTrigger(Map<String, GameActionList> regionActions, int interval, RunType runType) implements IGameBehavior {
	public static final MapCodec<WhileInRegionTrigger> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.unboundedMap(Codec.STRING, GameActionList.CODEC).fieldOf("regions").forGetter(WhileInRegionTrigger::regionActions),
			Codec.INT.optionalFieldOf("interval", 20).forGetter(WhileInRegionTrigger::interval),
			RunType.CODEC.optionalFieldOf("run_once", RunType.ALWAYS).forGetter(WhileInRegionTrigger::runType)
	).apply(i, WhileInRegionTrigger::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		State state = new State();
		for (GameActionList actions : regionActions.values()) {
			actions.register(game, events);
		}

		events.listen(GamePlayerEvents.TICK, player -> {
			if((runType == RunType.ONCE && state.triggered) || (runType == RunType.ONCE_PER_PLAYER && state.triggeredPlayers.contains(player.getUUID()))){
				return;
			}
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
					state.triggered = true;
					state.triggeredPlayers.add(player.getUUID());
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

	private static class State {
		public boolean triggered = false;
		public Set<UUID> triggeredPlayers = new HashSet<>();
	}

	public enum RunType implements StringRepresentable {

		ALWAYS("always"),
		ONCE("once"),
		ONCE_PER_PLAYER("once_per_player");

		final String id;

		public static final Codec<RunType> CODEC = StringRepresentable.fromEnum(RunType::values);
		RunType(String id){
			this.id = id;
		}

		@Override
		public String getSerializedName() {
			return id;
		}
	}
}
