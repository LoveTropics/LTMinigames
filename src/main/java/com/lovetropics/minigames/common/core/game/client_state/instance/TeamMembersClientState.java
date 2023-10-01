package com.lovetropics.minigames.common.core.game.client_state.instance;

import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;

import java.util.List;
import java.util.UUID;

public record TeamMembersClientState(List<UUID> teamMembers) implements GameClientState {
    public static final Codec<TeamMembersClientState> CODEC = UUIDUtil.CODEC.listOf().fieldOf("members")
            .xmap(TeamMembersClientState::new, TeamMembersClientState::teamMembers)
            .codec();

    @Override
    public GameClientStateType<?> getType() {
        return GameClientStateTypes.TEAM_MEMBERS.get();
    }
}
