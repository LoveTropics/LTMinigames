package com.lovetropics.minigames.common.core.game.behavior.instances.trigger.phase;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContext;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.mojang.serialization.Codec;

public record GameReadyTrigger(GameActionList<Void> actions) implements IGameBehavior {
    public static final Codec<GameReadyTrigger> CODEC = GameActionList.VOID
            .xmap(GameReadyTrigger::new, GameReadyTrigger::actions);
    
    @Override
    public void register(IGamePhase game, EventRegistrar events) throws GameException {
        actions.apply(game, GameActionContext.EMPTY);
    }
}