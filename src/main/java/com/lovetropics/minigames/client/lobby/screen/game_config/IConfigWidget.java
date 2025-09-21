package com.lovetropics.minigames.client.lobby.screen.game_config;

import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.ContainerEventHandler;

public interface IConfigWidget extends ContainerEventHandler, Renderable {

	int getHeight();
}
