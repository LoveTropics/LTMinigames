package org.lovetropics.games.lobbies.client.screen.flex;

public enum Axis {
	X, Y;

	public Axis cross() {
		return this == X ? Y : X;
	}
}
