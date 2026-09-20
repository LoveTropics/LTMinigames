package org.lovetropics.games.client.screen.flex;

public enum Axis {
	X, Y;

	public Axis cross() {
		return this == X ? Y : X;
	}
}
