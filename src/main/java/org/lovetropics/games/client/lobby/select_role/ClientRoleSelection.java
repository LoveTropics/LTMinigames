package org.lovetropics.games.client.lobby.select_role;

import net.minecraft.client.Minecraft;

public final class ClientRoleSelection {
	public static void openScreen(int lobbyId) {
		Minecraft.getInstance().setScreenAndShow(new SelectPlayerRoleScreen(lobbyId));
	}
}
