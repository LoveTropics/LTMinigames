package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.diguise.DisguiseType;
import com.lovetropics.minigames.common.core.diguise.ServerPlayerDisguises;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.mojang.serialization.MapCodec;

public record ClearDisguiseAction(DisguiseType disguise) implements IGameBehavior {
	public static final MapCodec<ClearDisguiseAction> CODEC = DisguiseType.MAP_CODEC.xmap(ClearDisguiseAction::new, ClearDisguiseAction::disguise);

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameActionEvents.APPLY_TO_PLAYER, (context, player) -> {
			ServerPlayerDisguises.clear(player, disguise);
			return true;
		});
	}
}
