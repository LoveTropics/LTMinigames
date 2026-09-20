package org.lovetropics.games.common.core.game.client_state.instance;

import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.util.List;

public record SidebarClientState(Component title, List<Component> lines) implements GameClientState {
	public static final MapCodec<SidebarClientState> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ComponentSerialization.CODEC.fieldOf("title").forGetter(SidebarClientState::title),
			ComponentSerialization.CODEC.listOf().fieldOf("lines").forGetter(SidebarClientState::lines)
	).apply(i, SidebarClientState::new));

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.SIDEBAR.get();
	}
}
