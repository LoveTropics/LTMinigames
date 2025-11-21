package com.lovetropics.minigames.common.content.escape_race.behaviours;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.context.ContextMap;

import java.util.Optional;

public record EarlyExitRoomAction(
		Optional<GameActionList> postCloseActions
) implements IGameBehavior {
	public static final MapCodec<EarlyExitRoomAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			GameActionList.CODEC.optionalFieldOf("post_close_actions").forGetter(EarlyExitRoomAction::postCloseActions)
	).apply(i, EarlyExitRoomAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		Warehouse warehouse = game.instanceState().getOrNull(WarehouseSetupBehaviour.KEY);
		if(warehouse == null) {
			return;
		}
		postCloseActions.ifPresent(actions -> {actions.register(game, events);});
		events.listen(GameActionEvents.APPLY, ((context, targets) -> {
			for (Warehouse.RoomInstance value : warehouse.rooms.values()) {
				if(value.isPlayingRoom()){
					GameTeamKey team = value.getTeam();
					value.earlyExit();
					if(team != null) {
						postCloseActions.ifPresent(actions -> {
							actions.apply(game, ContextMap.EMPTY, ActionSubjects.ofTeam(team));
						});
					}
				}
			}
			return true;
		}));
	}


}
