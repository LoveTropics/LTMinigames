package org.lovetropics.games.lobbies.client.screen.list;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.ObjectSelectionList.Entry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.lobbies.client.screen.list.AbstractLTList.Reorder;

import java.util.ArrayList;
import java.util.List;

public abstract class LTListEntry<T extends LTListEntry<T>> extends Entry<T> {
	public static final int ACTION_SIZE = 20;
	private static final int ACTION_SPACING = 2;
	private static final int ACTION_MARGIN = 4;
	private static final int ACTION_ICON_SIZE = 16;

	protected final Screen screen;
	protected final AbstractLTList<T> list;
	protected @Nullable Reorder reorder;
	protected int dragStartIndex;

	private final List<AbstractButton> actions = new ArrayList<>();
	private int focusedAction = -1;
	private boolean pressedAction;
	private @Nullable Runnable activate;

	public LTListEntry(AbstractLTList<T> list, Screen screen) {
		super();
		this.screen = screen;
		this.list = list;
	}

	public void renderTooltips(GuiGraphicsExtractor graphics, int width, int mouseX, int mouseY) {
	}

	public boolean hasSeparators() {
		return true;
	}

	public AbstractButton addAction(Component label, Component tooltip, Runnable onPress) {
		return addAction(Button.builder(label, button -> onPress.run())
				.size(ACTION_SIZE, ACTION_SIZE)
				.tooltip(Tooltip.create(tooltip))
				.build());
	}

	public AbstractButton addAction(Identifier sprite, Component tooltip, Runnable onPress) {
		return addAction(SpriteIconButton.builder(tooltip, button -> onPress.run(), true)
				.sprite(sprite, ACTION_ICON_SIZE, ACTION_ICON_SIZE)
				.size(ACTION_SIZE, ACTION_SIZE)
				.withTootip()
				.build());
	}

	public <B extends AbstractButton> B addAction(B button) {
		actions.add(button);
		return button;
	}

	protected boolean areActionsVisible(boolean hovered) {
		return !actions.isEmpty() && list.draggingEntry == null && (hovered || list.getSelected() == this);
	}

	protected int getVisibleActionsWidth(boolean hovered) {
		if (!areActionsVisible(hovered)) {
			return 0;
		}
		return actions.size() * ACTION_SIZE + (actions.size() - 1) * ACTION_SPACING + ACTION_MARGIN;
	}

	void extractActions(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
		if (!areActionsVisible(hovered)) {
			return;
		}
		layoutActions();
		for (int index = 0; index < actions.size(); index++) {
			AbstractButton action = actions.get(index);
			action.setFocused(index == focusedAction);
			action.extractRenderState(graphics, mouseX, mouseY, a);
		}
	}

	private void layoutActions() {
		int x = getContentRight() - ACTION_MARGIN - actions.size() * ACTION_SIZE - (actions.size() - 1) * ACTION_SPACING;
		int y = getContentYMiddle() - ACTION_SIZE / 2;
		for (AbstractButton action : actions) {
			action.setPosition(x, y);
			x += ACTION_SIZE + ACTION_SPACING;
		}
	}

	protected boolean isMouseOverAction(double mouseX, double mouseY) {
		return getActionAt(mouseX, mouseY) != null;
	}

	private @Nullable AbstractButton getActionAt(double mouseX, double mouseY) {
		if (!areActionsVisible(isMouseOver(mouseX, mouseY))) {
			return null;
		}
		layoutActions();
		for (AbstractButton action : actions) {
			if (action.visible && mouseX >= action.getX() && mouseY >= action.getY() && mouseX < action.getRight() && mouseY < action.getBottom()) {
				return action;
			}
		}
		return null;
	}

	void resetFocusedAction() {
		focusedAction = -1;
	}

	public void setActivateAction(@Nullable Runnable activate) {
		this.activate = activate;
	}

	boolean handleActionKey(KeyEvent event) {
		if (event.key() == InputConstants.KEY_RIGHT && focusedAction < actions.size() - 1) {
			focusedAction++;
			return true;
		}
		if (event.key() == InputConstants.KEY_LEFT && focusedAction >= 0) {
			focusedAction--;
			return true;
		}
		if (event.isSelection()) {
			if (focusedAction >= 0 && focusedAction < actions.size()) {
				layoutActions();
				return actions.get(focusedAction).keyPressed(event);
			}
			if (activate != null) {
				activate.run();
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		AbstractButton action = getActionAt(event.x(), event.y());
		boolean previousPressedAction = pressedAction;
		pressedAction = action != null;
		if (pressedAction) {
			action.mouseClicked(event, doubleClick);
			return true;
		}

		boolean doubleClickedEntry = doubleClick && !previousPressedAction && list.getSelected() == this;

		list.setSelected((T) this);
		dragStartIndex = list.children().indexOf(this);

		if (doubleClickedEntry && activate != null) {
			activate.run();
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (pressedAction) {
			return true;
		}
		if (reorder != null) {
			list.drag((T) this, event.y());
			return true;
		}
		return false;
	}
}
