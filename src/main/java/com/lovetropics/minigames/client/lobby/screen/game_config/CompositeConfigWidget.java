package com.lovetropics.minigames.client.lobby.screen.game_config;

import com.lovetropics.minigames.client.screen.LayoutGui;
import com.lovetropics.minigames.client.screen.LayoutTree;
import com.lovetropics.minigames.client.screen.flex.Box;
import com.lovetropics.minigames.common.core.game.behavior.config.ConfigData;
import com.lovetropics.minigames.common.core.game.behavior.config.ConfigData.CompositeConfigData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CompositeConfigWidget extends LayoutGui implements IConfigWidget {

	private final List<IConfigWidget> children = new ArrayList<>();

	public CompositeConfigWidget() {
		super();
	}

	public static CompositeConfigWidget from(GameConfig parent, LayoutTree ltree, CompositeConfigData data) {
		CompositeConfigWidget ret = new CompositeConfigWidget();
		for (Map.Entry<String, ConfigData> e : data.value().entrySet()) {
			ltree.child(new Box(5, 0, 0, 0), new Box(3, 3, 3, 3));
			ret.children.add(new ConfigDataUI(parent, ltree, e.getKey(), e.getValue()));
		}
		ret.mainLayout = ltree.pop();
		return ret;
	}

	@Override
	public List<? extends GuiEventListener> children() {
		return children;
	}

	@Override
	public int getHeight() {
		return mainLayout.margin().height();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		super.render(graphics, mouseX, mouseY, partialTicks);
		graphics.vLine(mainLayout.background().left(), mainLayout.background().top(), mainLayout.background().bottom(), -1);
		for (IConfigWidget child : children) {
			child.render(graphics, mouseX, mouseY, partialTicks);
		}
	}
}
