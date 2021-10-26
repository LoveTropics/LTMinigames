package com.lovetropics.minigames.client.lobby.screen.game_config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.lovetropics.minigames.client.screen.LayoutGui;
import com.lovetropics.minigames.client.screen.LayoutTree;
import com.lovetropics.minigames.client.screen.flex.Box;
import com.lovetropics.minigames.common.core.game.behavior.config.ConfigData;
import com.lovetropics.minigames.common.core.game.behavior.config.ConfigData.CompositeConfigData;
import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;

public class CompositeConfigWidget extends LayoutGui implements IConfigWidget {

	private final List<IConfigWidget> children = new ArrayList<>();

	public CompositeConfigWidget() {
		super();
	}

	public static CompositeConfigWidget from(Screen screen, LayoutTree ltree, CompositeConfigData data) {
		CompositeConfigWidget ret = new CompositeConfigWidget();
		for (Map.Entry<String, ConfigData> e : data.value().entrySet()) {
			ltree.child(new Box(5, 0, 0, 0), new Box(3, 3, 3, 3));
			ret.children.add(new ConfigDataUI(screen, ltree, e.getKey(), e.getValue()));
		}
		ret.mainLayout = ltree.pop();
		return ret;
	}

	@Override
	public List<? extends IGuiEventListener> getEventListeners() {
		return children;
	}

	@Override
	public int getHeight() {
		return children.stream().mapToInt(IConfigWidget::getHeight).sum();
	}
	
	@Override
	public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		super.render(matrixStack, mouseX, mouseY, partialTicks);
		for (IConfigWidget child : children) {
			child.render(matrixStack, mouseX, mouseY, partialTicks);
		}
	}
}
