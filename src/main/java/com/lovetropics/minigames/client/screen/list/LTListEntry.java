package com.lovetropics.minigames.client.screen.list;

import com.lovetropics.minigames.client.screen.list.AbstractLTList.Reorder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList.Entry;
import net.minecraft.client.gui.screens.Screen;

import javax.annotation.Nullable;

public abstract class LTListEntry<T extends LTListEntry<T>> extends Entry<T> {

	protected final Screen screen;
	protected final AbstractLTList<T> list;
	@Nullable
	protected Reorder reorder;
	protected int dragStartIndex;

	public LTListEntry(AbstractLTList<T> list, Screen screen) {
		super();
		this.screen = screen;
		this.list = list;
	}

	public void renderTooltips(GuiGraphics graphics, int width, int mouseX, int mouseY) {
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		list.setSelected((T) this);
		dragStartIndex = list.children().indexOf(this);
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (reorder != null) {
			list.drag((T) this, mouseY);
			return true;
		}
		return false;
	}
}
