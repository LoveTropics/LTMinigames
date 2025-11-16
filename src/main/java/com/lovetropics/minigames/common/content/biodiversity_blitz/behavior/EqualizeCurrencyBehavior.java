package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior;

import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.CurrencyManager;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.mojang.serialization.MapCodec;

public class EqualizeCurrencyBehavior implements IGameBehavior {
	public static final MapCodec<EqualizeCurrencyBehavior> CODEC = MapCodec.unit(EqualizeCurrencyBehavior::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		CurrencyManager currency = game.state().getOrThrow(CurrencyManager.KEY);

		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			currency.equalize();
			return true;
		});
	}
}
