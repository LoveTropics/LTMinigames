package org.lovetropics.games.lobbies.client.select_role;

import net.minecraft.client.Minecraft;
import org.lovetropics.games.lobbies.network.SelectPlayerRoleScreen;

public final class ClientRoleSelection {
	public static void openScreen(int lobbyId) {
		Minecraft.getInstance().setScreenAndShow(new SelectPlayerRoleScreen(lobbyId));
	}
}
