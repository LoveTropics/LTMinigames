package com.lovetropics.minigames.common.content.biodiversity_blitz.client_state;

import com.lovetropics.minigames.common.content.biodiversity_blitz.BiodiversityBlitz;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public record ClientBbScoreboardState(Vec3 start, Vec3 end, boolean side, Component header, List<Component> content) implements GameClientState {
    public static final Codec<ClientBbScoreboardState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Vec3.CODEC.fieldOf("start").forGetter(ClientBbScoreboardState::start),
            Vec3.CODEC.fieldOf("end").forGetter(ClientBbScoreboardState::end),
            Codec.BOOL.fieldOf("side").forGetter(ClientBbScoreboardState::side),
            ExtraCodecs.COMPONENT.fieldOf("header").forGetter(ClientBbScoreboardState::header),
            ExtraCodecs.COMPONENT.listOf().fieldOf("content").forGetter(ClientBbScoreboardState::content)
    ).apply(instance, ClientBbScoreboardState::new));

    @Override
    public GameClientStateType<?> getType() {
        return BiodiversityBlitz.SCOREBOARD.get();
    }
}
