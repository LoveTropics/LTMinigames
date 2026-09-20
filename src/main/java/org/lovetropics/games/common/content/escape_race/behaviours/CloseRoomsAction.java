package org.lovetropics.games.common.content.escape_race.behaviours;

import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record CloseRoomsAction(
		List<String> rooms
) implements IGameBehavior {
	public static final MapCodec<CloseRoomsAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.listOf().fieldOf("rooms").forGetter(CloseRoomsAction::rooms)
	).apply(i, CloseRoomsAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		Warehouse warehouse = game.instanceState().getOrNull(WarehouseSetupBehaviour.KEY);
		if(warehouse == null) {
			return;
		}
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			warehouse.removeRooms(rooms);
			return true;
		});
	}

}
