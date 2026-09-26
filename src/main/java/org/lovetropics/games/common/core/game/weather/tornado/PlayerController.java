package org.lovetropics.games.common.core.game.weather.tornado;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

public record PlayerController(UUID player, int lengthInTicks) {

    public static final Codec<PlayerController> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("player").forGetter(PlayerController::player),
            Codec.INT.fieldOf("length_in_ticks").forGetter(PlayerController::lengthInTicks)
    ).apply(i, PlayerController::new));

}
