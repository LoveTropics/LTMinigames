package com.lovetropics.minigames.common.core.game.client_state.instance;

import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateType;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

public record DisableRecipeBookClientState(Component message) implements GameClientState {
	public static final MapCodec<DisableRecipeBookClientState> CODEC = ComponentSerialization.CODEC
			.fieldOf("message").xmap(DisableRecipeBookClientState::new, DisableRecipeBookClientState::message);

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.DISABLE_RECIPE_BOOK.get();
	}
}
