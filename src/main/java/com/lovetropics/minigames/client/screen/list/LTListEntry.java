package com.lovetropics.minigames.client.screen.list;

import com.lovetropics.minigames.client.screen.list.AbstractLTList.Reorder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList.Entry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

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

	public void renderTooltips(GuiGraphicsExtractor graphics, int width, int mouseX, int mouseY) {
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		list.setSelected((T) this);
		dragStartIndex = list.children().indexOf(this);
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (reorder != null) {
			list.drag((T) this, event.y());
			return true;
		}
		return false;
	}
}
