package org.lovetropics.games.lobbies;

import net.minecraft.network.chat.Component;

public enum LobbyVisibility {
	PUBLIC(GameLobbyTexts.Ui.LOBBY_PUBLIC),
	PUBLIC_LIVE(GameLobbyTexts.Ui.LOBBY_PUBLIC_LIVE),
	PRIVATE(GameLobbyTexts.Ui.LOBBY_PRIVATE);

	private final Component name;

	LobbyVisibility(Component name) {
		this.name = name;
	}

	public boolean isPublic() {
		return !isPrivate();
	}

	public boolean isPrivate() {
		return this == PRIVATE;
	}

	public boolean isFocusedLive() {
		return this == PUBLIC_LIVE;
	}

	public Component getName() {
		return name;
	}
}
