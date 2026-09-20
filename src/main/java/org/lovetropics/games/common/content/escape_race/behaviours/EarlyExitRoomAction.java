package org.lovetropics.games.common.content.escape_race.behaviours;

import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import com.mojang.serialization.MapCodec;

public record EarlyExitRoomAction() implements IGameBehavior {
	public static final MapCodec<EarlyExitRoomAction> CODEC = MapCodec.unit(EarlyExitRoomAction::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		Warehouse warehouse = game.instanceState().getOrNull(WarehouseSetupBehaviour.KEY);
		if (warehouse == null) {
			return;
		}
		events.listen(GameActionEvents.APPLY, ((context, targets) -> {
			for (Warehouse.RoomInstance value : warehouse.rooms.values()) {
				if (value.isPlayingRoom()) {
					value.earlyExit();
				}
			}
			return true;
		}));
	}
}
