package com.lovetropics.minigames.common.core.game.util;

import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.game.client_state.instance.SidebarClientState;
import com.lovetropics.minigames.common.core.game.player.MutablePlayerSet;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

public final class GameSidebar implements GameWidget {
	private final GameWidgets widgets;
	private final MutablePlayerSet players;
	private final Component title;

	private Component[] display = new Component[0];

	public GameSidebar(GameWidgets widgets, MinecraftServer server, Component title) {
		this.widgets = widgets;
		players = new MutablePlayerSet(server);
		this.title = title;
	}

	public void set(Component[] display) {
		if (!Arrays.equals(this.display, display)) {
			this.display = display;
			GameClientState.sendToPlayers(buildState(), players);
		}
	}

	@Override
	public void addPlayer(ServerPlayer player) {
		players.add(player);
		GameClientState.sendToPlayer(buildState(), player);
	}

	@Override
	public void removePlayer(ServerPlayer player) {
		players.remove(player);
		GameClientState.removeFromPlayer(GameClientStateTypes.SIDEBAR.get(), player);
	}

	private SidebarClientState buildState() {
		return new SidebarClientState(title, Arrays.asList(display));
	}

	@Override
	public void close() {
		GameClientState.removeFromPlayers(GameClientStateTypes.SIDEBAR.get(), players);
		players.clear();
		widgets.remove(this);
	}
}
