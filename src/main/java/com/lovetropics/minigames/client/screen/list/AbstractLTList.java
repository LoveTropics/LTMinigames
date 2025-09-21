package com.lovetropics.minigames.client.screen.list;

import com.lovetropics.minigames.client.screen.flex.Layout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.List;

public abstract class AbstractLTList<T extends LTListEntry<T>> extends ObjectSelectionList<T> {

	private static final int SCROLL_WIDTH = 6;
	public final Screen screen;
	@Nullable
	protected T draggingEntry;
	private int dragOffset;

	public interface Reorder {
		void onReorder(int offset);
	}

	public AbstractLTList(Screen screen, Layout layout, int entryHeight) {
		super(
				screen.getMinecraft(),
				layout.background().width(), layout.background().height(),
				layout.background().top(),
				entryHeight
		);
		this.screen = screen;
		setPosition(layout.background().left(), layout.background().top());
	}

	public AbstractLTList(Screen screen, Layout layout, int entryHeight, int headerHeight) {
		super(
				screen.getMinecraft(),
				layout.background().width(), layout.background().height(),
				layout.background().top(),
				entryHeight,
				headerHeight
		);
		this.screen = screen;
		setPosition(layout.background().left(), layout.background().top());
	}

	public void renderOverlays(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		renderDragging(graphics, mouseX, mouseY, partialTicks);
		renderTooltips(graphics, mouseX, mouseY);
	}

	protected void renderDragging(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		T dragging = draggingEntry;
		if (dragging != null) {
			int index = children().indexOf(dragging);
			int y = getDraggingY(mouseY);
			dragging.render(graphics, index, y, getRowLeft(), getRowWidth(), itemHeight, mouseX, mouseY, false, partialTicks);
		}
	}

	protected void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
		if (!isMouseOver(mouseX, mouseY) || draggingEntry != null) {
			return;
		}

		int count = getItemCount();
		int rowWidth = getRowWidth();

		for (int index = 0; index < count; index++) {
			int rowTop = getRowTop(index);
			int rowBottom = rowTop + itemHeight;
			if (rowBottom < getY() || rowTop > getY() + getHeight()) {
				continue;
			}

			T entry = getEntry(index);
			if (isMouseOverEntry(mouseX, mouseY, entry)) {
				entry.renderTooltips(graphics, rowWidth, mouseX, mouseY);
				break;
			}
		}
	}

	private boolean isMouseOverEntry(int mouseX, int mouseY, T entry) {
		return getEntryAtPosition(mouseX, mouseY) == entry;
	}

	private int getEntryIndexAt(int y) {
		int contentY = y - getY() - headerHeight + (int) scrollAmount();
		return contentY / itemHeight;
	}

	public abstract void updateEntries();

	@Override
	public int getRowLeft() {
		return getX();
	}

	@Override
	public int getRowWidth() {
		return maxScrollAmount() > 0 ? width - SCROLL_WIDTH : width;
	}

	@Override
	protected int scrollBarX() {
		return maxScrollAmount() > 0 ? getX() + getWidth() - SCROLL_WIDTH : getX() + getWidth();
	}

	@Override
	public int getRowTop(int index) {
		return getY() + headerHeight - (int) scrollAmount()
				+ index * itemHeight;
	}

	void drag(T entry, double mouseY) {
		if (draggingEntry != entry) {
			startDragging(entry, mouseY);
		} else {
			int insertIndex = getDragInsertIndex(Mth.floor(mouseY));
			tryReorderTo(entry, insertIndex);
		}
	}

	private void startDragging(T entry, double mouseY) {
		draggingEntry = entry;

		int index = children().indexOf(entry);
		dragOffset = Mth.floor(getRowTop(index) - mouseY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		T dragging = draggingEntry;
		if (dragging != null) {
			stopDragging(dragging);
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		T selected = getSelected();
		if (selected != null && selected.reorder != null && Screen.hasShiftDown()) {
			int offset = 0;
			if (keyCode == GLFW.GLFW_KEY_UP) {
				offset = -1;
			} else if (keyCode == GLFW.GLFW_KEY_DOWN) {
				offset = 1;
			}

			if (offset != 0) {
				int index = children().indexOf(selected);
				if (tryReorderTo(selected, index + offset)) {
					selected.reorder.onReorder(offset);
					return true;
				}
			}
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	protected int getDraggingY(int mouseY) {
		int draggingY = mouseY + dragOffset;
		int minY = getY() + headerHeight;
		int maxY = getY() + getHeight() - itemHeight;
		return Mth.clamp(draggingY, minY, maxY);
	}

	private int getDragInsertIndex(int mouseY) {
		return getEntryIndexAt(getDraggingY(mouseY) + itemHeight / 2);
	}

	private boolean tryReorderTo(T entry, int insertIndex) {
		List<T> entries = children();
		int index = entries.indexOf(entry);
		if (index == -1) {
			return false;
		}

		if (insertIndex != index && insertIndex >= 0 && insertIndex < entries.size()) {
			T replaceEntry = entries.get(insertIndex);
			if (replaceEntry.reorder != null) {
				entries.remove(index);
				entries.add(insertIndex, entry);
				return true;
			}
		}
		return false;
	}

	private void stopDragging(T dragging) {
		int startIndex = dragging.dragStartIndex;
		int index = children().indexOf(dragging);
		if (startIndex != index && dragging.reorder != null) {
			dragging.reorder.onReorder(index - startIndex);
		}
		draggingEntry = null;
	}

	@Override
	public void setSelected(@Nullable T entry) {
		T dragging = draggingEntry;
		if (entry == null && dragging != null && dragging == getSelected()) {
			stopDragging(dragging);
		}
		super.setSelected(entry);
	}

	@Override
	protected void renderListItems(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		boolean listHovered = isMouseOver(mouseX, mouseY);

		int count = getItemCount();
		int left = getRowLeft();
		int width = getRowWidth();
		int height = itemHeight;

		boolean dragging = draggingEntry != null;

		for (int index = 0; index < count; index++) {
			int top = getRowTop(index);
			int bottom = top + height;
			if (bottom < getY() || top > getY() + getHeight()) {
				continue;
			}

			T entry = getEntry(index);
			if (draggingEntry == entry) {
				continue;
			}

			boolean entryHovered = !dragging && listHovered && mouseX >= left && mouseY >= top && mouseX < left + width && mouseY < bottom;
			entry.render(graphics, index, top, left, width, height, mouseX, mouseY, entryHovered, partialTicks);
		}
	}
}
