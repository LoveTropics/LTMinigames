package com.lovetropics.minigames.client.lobby.select_role;

import net.minecraft.client.Minecraft;

public final class ClientRoleSelection {
	public static void openScreen(int lobbyId) {
		Minecraft.getInstance().setScreen(new SelectPlayerRoleScreen(lobbyId));
	}
}
