package com.lovetropics.minigames.common.core.integration.game_actions;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePackageEvents;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.TriState;
import org.slf4j.Logger;

import java.util.Optional;

public record ChatEventGameAction(String trigger) implements GameAction {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final MapCodec<ChatEventGameAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("trigger").forGetter(ChatEventGameAction::trigger)
    ).apply(i, ChatEventGameAction::new));

    // TODO: Make GamePackage system less specific to packages
    @Override
    public boolean resolve(IGamePhase game, MinecraftServer server) {
        GamePackage triggeredPackage = new GamePackage(trigger, "", Optional.empty(), Optional.empty());

        TriState result = game.invoker(GamePackageEvents.RECEIVE_PACKAGE).onReceivePackage(triggeredPackage);
        switch (result) {
			case TRUE -> LOGGER.debug("Incoming chat event was successfully processed by behavior: {}", triggeredPackage);
			case DEFAULT -> LOGGER.debug("Incoming chat event was not handled by behavior: {}", triggeredPackage);
			case FALSE -> LOGGER.debug("Incoming chat event was rejected by behavior: {}", triggeredPackage);
        }

        return result.isTrue();
    }
}
