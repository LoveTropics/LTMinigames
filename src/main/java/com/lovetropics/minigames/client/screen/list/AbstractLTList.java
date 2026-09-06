package com.lovetropics.minigames.client.screen.list;

import com.lovetropics.minigames.client.screen.flex.Layout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public abstract class AbstractLTList<T extends LTListEntry<T>> extends ObjectSelectionList<T> {

	private static final int SCROLL_WIDTH = 6;
	private static final int LIST_PADDING = 2;

	public final Screen screen;
	protected @Nullable T draggingEntry;
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

	public void renderOverlays(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		renderDragging(graphics, mouseX, mouseY, partialTicks);
		renderTooltips(graphics, mouseX, mouseY);
	}

	protected void renderDragging(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		T dragging = draggingEntry;
		if (dragging == null) {
			return;
		}

		int restoreY = dragging.getY();
		dragging.setY(getDraggingY(mouseY));
		dragging.extractContent(graphics, mouseX, mouseY, true, partialTicks);
		dragging.setY(restoreY);
	}

	protected void renderTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		if (!isMouseOver(mouseX, mouseY) || draggingEntry != null) {
			return;
		}

		int rowWidth = getRowWidth();

		for (T entry : children()) {
			if (entry.getY() + entry.getHeight() < getY() || entry.getY() > getBottom()) {
				continue;
			}

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
		List<T> entries = children();
		for (int index = 0; index < entries.size(); index++) {
			T entry = entries.get(index);
			if (y < entry.getY() + entry.getHeight()) {
				return index;
			}
		}
		return entries.size() - 1;
	}

	public abstract void updateEntries();

	@Override
	protected int addEntry(T entry, int height) {
		int index = super.addEntry(entry, height);
		refreshEntryBounds();
		return index;
	}

	private void refreshEntryBounds() {
		int rowLeft = getRowLeft();
		int rowWidth = getRowWidth();
		int y = getY() + LIST_PADDING - (int) scrollAmount();
		for (T entry : children()) {
			entry.setX(rowLeft);
			entry.setWidth(rowWidth);
			entry.setY(y);
			y += entry.getHeight();
		}
	}

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
	public boolean mouseReleased(MouseButtonEvent event) {
		T dragging = draggingEntry;
		if (dragging != null) {
			stopDragging(dragging);
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		T selected = getSelected();
		if (selected != null && selected.reorder != null && Minecraft.getInstance().hasShiftDown()) {
			int offset = 0;
			if (event.key() == GLFW.GLFW_KEY_UP) {
				offset = -1;
			} else if (event.key() == GLFW.GLFW_KEY_DOWN) {
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

		return super.keyPressed(event);
	}

	protected int getDraggingY(int mouseY) {
		int draggingHeight = getDraggingHeight();
		int minY = getY() + LIST_PADDING;
		int maxY = getBottom() - draggingHeight;
		return Mth.clamp(mouseY + dragOffset, minY, Math.max(minY, maxY));
	}

	private int getDragInsertIndex(int mouseY) {
		return getEntryIndexAt(getDraggingY(mouseY) + getDraggingHeight() / 2);
	}

	private int getDraggingHeight() {
		T dragging = draggingEntry;
		return dragging != null ? dragging.getHeight() : defaultEntryHeight;
	}

	private boolean tryReorderTo(T entry, int insertIndex) {
		List<T> entries = children();
		int index = entries.indexOf(entry);
		if (index == -1 || insertIndex == index || insertIndex < 0 || insertIndex >= entries.size()) {
			return false;
		}

		int step = insertIndex > index ? 1 : -1;
		boolean reordered = false;
		for (int from = index; from != insertIndex; from += step) {
			int to = from + step;
			if (entries.get(to).reorder == null) {
				break;
			}
			swap(from, to);
			reordered = true;
		}
		return reordered;
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
	protected void extractListItems(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		boolean listHovered = isMouseOver(mouseX, mouseY);
		boolean dragging = draggingEntry != null;

		for (T entry : children()) {
			if (entry.getY() + entry.getHeight() < getY() || entry.getY() > getBottom()) {
				continue;
			}

			if (draggingEntry == entry) {
				continue;
			}

			boolean entryHovered = !dragging && listHovered && entry.isMouseOver(mouseX, mouseY);
			entry.extractContent(graphics, mouseX, mouseY, entryHovered, a);
		}
	}
}
