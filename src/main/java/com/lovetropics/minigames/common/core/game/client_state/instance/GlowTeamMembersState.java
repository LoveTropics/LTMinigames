package com.lovetropics.minigames.common.core.game.client_state.instance;

import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;

public class GlowTeamMembersState implements GameClientState {
	public static final GlowTeamMembersState INSTANCE = new GlowTeamMembersState();

	private GlowTeamMembersState() {
	}

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.GLOW_TEAM_MEMBERS.get();
	}
}
