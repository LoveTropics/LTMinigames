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
	protected T draggingEntry;
	private int dragOffset;

	public interface Reorder {
		void onReorder(int offset);
	}
	
	public AbstractLTList(Screen screen, Layout layout, int slotHeightIn) {
		super(
				screen.getMinecraft(),
				layout.background().width(), layout.background().height(),
				layout.background().top(),
				slotHeightIn
		);
		this.screen = screen;
		this.setPosition(layout.background().left(), layout.background().top());
	}

	public void renderOverlays(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		renderDragging(graphics, mouseX, mouseY, partialTicks);
		renderTooltips(graphics, mouseX, mouseY);
	}

	protected void renderDragging(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		T dragging = this.draggingEntry;
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

		int count = this.getItemCount();
		int rowWidth = this.getRowWidth();

		for (int index = 0; index < count; index++) {
			int rowTop = this.getRowTop(index);
			int rowBottom = rowTop + this.itemHeight;
			if (rowBottom < this.getY() || rowTop > this.getY() + this.getHeight()) {
				continue;
			}

			T entry = this.getEntry(index);
			if (isMouseOverEntry(mouseX, mouseY, entry)) {
				entry.renderTooltips(graphics, rowWidth, mouseX, mouseY);
				break;
			}
		}
	}

	private boolean isMouseOverEntry(int mouseX, int mouseY, T entry) {
		return this.getEntryAtPosition(mouseX, mouseY) == entry;
	}

	private int getEntryIndexAt(int y) {
		int contentY = y - this.getY() - this.headerHeight + (int) this.getScrollAmount();
		return contentY / this.itemHeight;
	}

	public abstract void updateEntries();

	@Override
	public int getRowLeft() {
		return this.getX();
	}

	@Override
	public int getRowWidth() {
		return this.getMaxScroll() > 0 ? this.width - SCROLL_WIDTH : this.width;
	}

	@Override
	protected int getScrollbarPosition() {
		return this.getMaxScroll() > 0 ? this.getX() + this.getWidth() - SCROLL_WIDTH : this.getX() + this.getWidth();
	}

	@Override
	protected int getRowTop(int index) {
		return this.getY() + this.headerHeight - (int) this.getScrollAmount()
				+ index * this.itemHeight;
	}

	void drag(T entry, double mouseY) {
		if (this.draggingEntry != entry) {
			this.startDragging(entry, mouseY);
		} else {
			int insertIndex = this.getDragInsertIndex(Mth.floor(mouseY));
			this.tryReorderTo(entry, insertIndex);
		}
	}

	private void startDragging(T entry, double mouseY) {
		this.draggingEntry = entry;
	
		int index = this.children().indexOf(entry);
		this.dragOffset = Mth.floor(this.getRowTop(index) - mouseY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		T dragging = this.draggingEntry;
		if (dragging != null) {
			this.stopDragging(dragging);
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		T selected = getSelected();
		if (selected != null && selected.reorder != null && Screen.hasShiftDown()) {
			int offset = 0;
			if (keyCode == GLFW.GLFW_KEY_UP) offset = -1;
			else if (keyCode == GLFW.GLFW_KEY_DOWN) offset = 1;

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
		int minY = this.getY() + this.headerHeight;
		int maxY = this.getY() + this.getHeight() - this.itemHeight;
		return Mth.clamp(draggingY, minY, maxY);
	}

	private int getDragInsertIndex(int mouseY) {
		return getEntryIndexAt(getDraggingY(mouseY) + itemHeight / 2);
	}

	private boolean tryReorderTo(T entry, int insertIndex) {
		List<T> entries = children();
		int index = entries.indexOf(entry);
		if (index == -1) return false;

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
		int index = this.children().indexOf(dragging);
		if (startIndex != index && dragging.reorder != null) {
			dragging.reorder.onReorder(index - startIndex);
		}
		this.draggingEntry = null;
	}

	@Override
	public void setSelected(@Nullable T entry) {
		T dragging = this.draggingEntry;
		if (entry == null && dragging != null && dragging == this.getSelected()) {
			this.stopDragging(dragging);
		}
		super.setSelected(entry);
	}

	@Override
	protected void renderListItems(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		boolean listHovered = this.isMouseOver(mouseX, mouseY);
	
		int count = this.getItemCount();
		int left = this.getRowLeft();
		int width = this.getRowWidth();
		int height = this.itemHeight;
	
		boolean dragging = draggingEntry != null;
	
		for (int index = 0; index < count; index++) {
			int top = this.getRowTop(index);
			int bottom = top + height;
			if (bottom < this.getY() || top > this.getY() + this.getHeight()) continue;
	
			T entry = this.getEntry(index);
			if (draggingEntry == entry) continue;
	
			boolean entryHovered = !dragging && listHovered && mouseX >= left && mouseY >= top && mouseX < left + width && mouseY < bottom;
			entry.render(graphics, index, top, left, width, height, mouseX, mouseY, entryHovered, partialTicks);
		}
	}
}
