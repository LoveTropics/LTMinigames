package com.lovetropics.minigames.client.lobby.screen.game_config;

import com.lovetropics.minigames.client.lobby.state.ClientConfigList;
import com.lovetropics.minigames.client.screen.LayoutGui;
import com.lovetropics.minigames.client.screen.LayoutTree;
import com.lovetropics.minigames.common.core.game.behavior.config.BehaviorConfig;
import com.lovetropics.minigames.common.core.game.behavior.config.ConfigData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BehaviorConfigList extends LayoutGui {

	private final ClientConfigList configList;

	private final List<ConfigDataUI> children = new ArrayList<>();

	public BehaviorConfigList(GameConfig parent, LayoutTree layout, ClientConfigList configs) {
		super();
		configList = configs;

		updateEntries(parent, layout);
	}

	public void updateEntries(GameConfig parent, LayoutTree ltree) {
		Map<BehaviorConfig<?>, ConfigData> configs = configList.configs();

		for (Map.Entry<BehaviorConfig<?>, ConfigData> e : configs.entrySet()) {
			ConfigDataUI listEntry = new ConfigDataUI(parent, ltree.child(0, 3), e.getKey().getName(), e.getValue());
			children.add(listEntry);
		}
		mainLayout = ltree.pop();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		super.render(graphics, mouseX, mouseY, partialTicks);
		for (ConfigDataUI child : children) {
			child.render(graphics, mouseX, mouseY, partialTicks);
		}
	}

	public int getHeight() {
		return children.stream().mapToInt(ConfigDataUI::getHeight).sum();
	}

	@Override
	public List<? extends GuiEventListener> children() {
		return children;
	}
}
