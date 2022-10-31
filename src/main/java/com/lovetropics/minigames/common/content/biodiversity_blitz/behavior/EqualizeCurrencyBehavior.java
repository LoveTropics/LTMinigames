package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior;

import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.CurrencyManager;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.mojang.serialization.Codec;

public class EqualizeCurrencyBehavior implements IGameBehavior {
    public static final Codec<EqualizeCurrencyBehavior> CODEC = Codec.unit(EqualizeCurrencyBehavior::new);

    @Override
    public void register(IGamePhase game, EventRegistrar events) throws GameException {
        CurrencyManager currency = game.getState().getOrThrow(CurrencyManager.KEY);

        events.listen(GameActionEvents.APPLY, (context, sources) -> {
            currency.equalize();
            return true;
        });
    }
}
