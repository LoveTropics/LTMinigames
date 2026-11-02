package org.lovetropics.games.lobbies;

public enum LobbyVisibility {
	PUBLIC,
	PRIVATE;

	public boolean isPublic() {
		return !isPrivate();
	}

	public boolean isPrivate() {
		return this == PRIVATE;
	}
}
