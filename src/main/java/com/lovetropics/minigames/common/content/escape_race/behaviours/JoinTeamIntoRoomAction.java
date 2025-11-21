package com.lovetropics.minigames.common.content.escape_race.behaviours;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

public record JoinTeamIntoRoomAction(
		Optional<GameTeamKey> teamKey,
		String roomName
) implements IGameBehavior {
	public static final MapCodec<JoinTeamIntoRoomAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			GameTeamKey.CODEC.optionalFieldOf("team").forGetter(JoinTeamIntoRoomAction::teamKey),
			Codec.STRING.fieldOf("room").forGetter(JoinTeamIntoRoomAction::roomName)
	).apply(i, JoinTeamIntoRoomAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		Warehouse warehouse = game.instanceState().getOrNull(WarehouseSetupBehaviour.KEY);
		if(warehouse == null) {
			return;
		}
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			warehouse.joinIntoRoom(warehouse.rooms.get(roomName), teamKey.orElse(null) );
			return true;
		});
	}

}
