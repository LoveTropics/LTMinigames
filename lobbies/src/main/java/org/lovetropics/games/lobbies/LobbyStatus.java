package org.lovetropics.games.lobbies;

import net.minecraft.ChatFormatting;

public enum LobbyStatus {
	WAITING("waiting", ChatFormatting.GOLD),
	PLAYING("in progress", ChatFormatting.GREEN),
	PAUSED("paused", ChatFormatting.RED);

	public final String description;
	public final ChatFormatting color;

	LobbyStatus(String description, ChatFormatting color) {
		this.description = description;
		this.color = color;
	}
}
