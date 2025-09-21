package com.lovetropics.minigames.client.screen;

import com.lovetropics.minigames.client.screen.flex.Layout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;

public abstract class LayoutGui extends AbstractContainerEventHandler implements Renderable {

	protected Layout mainLayout;

	public LayoutGui() {
		this(null);
	}

	public LayoutGui(Layout mainLayout) {
		this.mainLayout = mainLayout;
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return mainLayout.padding().contains(mouseX, mouseY);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		mainLayout.debugRender(graphics);
	}
}
