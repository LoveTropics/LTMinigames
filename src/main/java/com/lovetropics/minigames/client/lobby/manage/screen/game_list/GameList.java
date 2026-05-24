package com.lovetropics.minigames.client.lobby.manage.screen.game_list;

import com.lovetropics.minigames.client.lobby.manage.state.ClientLobbyManageState;
import com.lovetropics.minigames.client.screen.flex.Layout;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

// TODO: name?
public final class GameList implements GuiEventListener, NarratableEntry {
	private final Screen screen;
	private final Layout mainLayout;
	private final Layout footerLayout;

	private final ClientLobbyManageState lobby;

	private final Handlers handlers;

	private AbstractGameList active;

	public GameList(
			Screen screen, Layout main, Layout footer,
			ClientLobbyManageState lobby,
			Handlers handlers
	) {
		this.screen = screen;
		mainLayout = main;
		footerLayout = footer;
		this.lobby = lobby;
		this.handlers = handlers;

		setActive(createQueue());
	}

	private GameQueueList createQueue() {
		return new GameQueueList(screen, mainLayout, footerLayout, lobby, new GameQueueList.Handlers() {
			@Override
			public void select(int id) {
				handlers.selectQueuedGame(id);
			}

			@Override
			public void enqueue() {
				setActive(createInstalled());
			}

			@Override
			public void remove(int id) {
				handlers.removeQueuedGame(id);
			}

			@Override
			public void reorder(int id, int offset) {
				handlers.reorderQueuedGame(id, offset);
			}
		});
	}

	private InstalledGameList createInstalled() {
		return new InstalledGameList(screen, mainLayout, footerLayout, lobby, index -> {
			handlers.selectQueuedGame(-1);
			handlers.enqueueGame(index);

			setActive(createQueue());
		});
	}

	private void setActive(AbstractGameList active) {
		active.updateEntries();
		this.active = active;
	}

	public void updateEntries() {
		active.updateEntries();
	}

	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		active.extractRenderState(graphics, mouseX, mouseY, partialTicks);
	}

	public void renderOverlays(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		active.renderOverlays(graphics, mouseX, mouseY, partialTicks);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		active.mouseMoved(mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		return active.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		return active.mouseReleased(event);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		return active.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
		return active.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		return active.keyPressed(event);
	}

	@Override
	public boolean keyReleased(KeyEvent event) {
		return active.keyReleased(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		return active.charTyped(event);
	}

	@Nullable
	@Override
	public ComponentPath nextFocusPath(FocusNavigationEvent event) {
		return active.nextFocusPath(event);
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return active.isMouseOver(mouseX, mouseY);
	}

	@Override
	public void setFocused(boolean focused) {
		active.setFocused(focused);
	}

	@Override
	public boolean isFocused() {
		return active.isFocused();
	}

	@Override
	public NarrationPriority narrationPriority() {
		return active.narrationPriority();
	}

	@Override
	public void updateNarration(final NarrationElementOutput output) {
		active.updateNarration(output);
	}

	public Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
		if (active.isMouseOver(mouseX, mouseY)) {
			return Optional.of(active);
		}
		return Optional.empty();
	}

	public interface Handlers {
		void selectQueuedGame(int queuedGameId);

		void enqueueGame(int installedGameIndex);

		void removeQueuedGame(int queuedGameId);

		void reorderQueuedGame(int queuedGameId, int offset);
	}
}
