package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContext;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

public record OnDamageTrigger(GameActionList<ServerPlayer> actions) implements IGameBehavior {
    public static final MapCodec<OnDamageTrigger> CODEC = GameActionList.PLAYER_MAP_CODEC.xmap(OnDamageTrigger::new, OnDamageTrigger::actions);

    @Override
    public void register(IGamePhase game, EventRegistrar events) throws GameException {
        actions.register(game, events);

        events.listen(GamePlayerEvents.DAMAGE, (player, damageSource, amount) -> {
            actions.apply(game, GameActionContext.EMPTY, player);
            return InteractionResult.PASS;
        });
    }
}
