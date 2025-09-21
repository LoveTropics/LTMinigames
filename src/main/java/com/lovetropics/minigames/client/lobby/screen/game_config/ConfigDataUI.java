package com.lovetropics.minigames.client.lobby.screen.game_config;

import com.lovetropics.minigames.client.screen.LayoutGui;
import com.lovetropics.minigames.client.screen.LayoutTree;
import com.lovetropics.minigames.client.screen.flex.Align;
import com.lovetropics.minigames.client.screen.flex.Axis;
import com.lovetropics.minigames.common.core.game.behavior.config.ConfigData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ConfigDataUI extends LayoutGui implements IConfigWidget {

	private final GameConfig parent;
	private final String name;
	private final ConfigData configs;
	private final TextLabel label;

	private final List<ContainerEventHandler> children = new ArrayList<>();

	public ConfigDataUI(GameConfig parent, LayoutTree ltree, String name, ConfigData configs) {
		super();
		this.parent = parent;
		this.name = name;
		this.configs = configs;

		label = new TextLabel(ltree.child(1, Axis.X), 11, Component.translatable(name), Align.Cross.START, Align.Cross.START);
		IConfigWidget widget = parent.createWidget(ltree.child(1, Axis.X), configs);
		children.add(widget);
		mainLayout = ltree.pop();
	}

	@Override
	public List<? extends GuiEventListener> children() {
		return children;
	}

	@Override
	public int getHeight() {
		return mainLayout.background().height();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		super.render(graphics, mouseX, mouseY, partialTicks);
		label.render(graphics, mouseX, mouseY, partialTicks);
		for (ContainerEventHandler child : children) {
			if (child instanceof Renderable renderable) {
				renderable.render(graphics, mouseX, mouseY, partialTicks);
			}
		}
	}
}
