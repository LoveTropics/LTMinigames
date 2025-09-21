package com.lovetropics.minigames.client.screen;

import com.lovetropics.minigames.client.screen.flex.Flex;
import com.lovetropics.minigames.client.screen.flex.FlexSolver;
import com.lovetropics.minigames.client.screen.flex.Layout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;

public abstract class DynamicLayoutGui extends AbstractContainerEventHandler implements Renderable {

	private final Flex basis;
	protected Layout mainLayout;

	public DynamicLayoutGui(Flex basis) {
		this.basis = basis;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		mainLayout.debugRender(graphics);
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return mainLayout.padding().contains(mouseX, mouseY);
	}

	public void bake(FlexSolver.Results solve) {
		mainLayout = solve.layout(basis);
		for (GuiEventListener child : children()) {
			if (child instanceof DynamicLayoutGui) {
				((DynamicLayoutGui) child).bake(solve);
			}
		}
	}
}
