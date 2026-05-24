package com.lovetropics.minigames.client.lobby.manage.screen.game_list;

import com.lovetropics.minigames.client.lobby.manage.state.ClientLobbyManageState;
import com.lovetropics.minigames.client.lobby.manage.state.ClientLobbyQueue;
import com.lovetropics.minigames.client.lobby.state.ClientCurrentGame;
import com.lovetropics.minigames.client.screen.FlexUi;
import com.lovetropics.minigames.client.screen.flex.Flex;
import com.lovetropics.minigames.client.screen.flex.FlexSolver;
import com.lovetropics.minigames.client.screen.flex.Layout;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import javax.annotation.Nullable;
import java.util.Optional;

public final class GameQueueList extends AbstractGameList {
	private static final Component TITLE = GameTexts.Ui.GAME_QUEUE.copy()
			.withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD);

	private final ClientLobbyManageState lobby;

	private final Handlers handlers;

	private final Button enqueueButton;
	private final Button removeButton;

	public GameQueueList(Screen screen, Layout main, Layout footer, ClientLobbyManageState lobby, Handlers handlers) {
		super(screen, main, TITLE);
		this.lobby = lobby;
		this.handlers = handlers;

		Flex root = new Flex().row();
		Flex enqueue = root.child().size(20, 20).marginRight(2);
		Flex cancel = root.child().size(20, 20).marginLeft(2);

		FlexSolver.Results solve = new FlexSolver(footer.content()).apply(root);
		enqueueButton = FlexUi.createButton(solve.layout(enqueue), Component.literal("+"), this::enqueue);
		removeButton = FlexUi.createButton(solve.layout(cancel), Component.literal("-"), this::remove);
	}

	@Override
	public Optional<GuiEventListener> getChildAt(double x, double y) {
		if (enqueueButton.isMouseOver(x, y)) {
			return Optional.of(enqueueButton);
		} else if (removeButton.isMouseOver(x, y)) {
			return Optional.of(removeButton);
		}
		return super.getChildAt(x, y);
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return enqueueButton.isMouseOver(mouseX, mouseY) || removeButton.isMouseOver(mouseX, mouseY) || super.isMouseOver(mouseX, mouseY);
	}

	@Override
	public void updateEntries() {
		int selectedId = getSelected() != null ? getSelected().getId() : -1;

		clearEntries();

		addEntry(createCurrentGameEntry(lobby.getCurrentGame()));

		for (ClientLobbyQueue.Entry entry : lobby.getQueue().entries()) {
			int id = entry.id();
			Entry listEntry = Entry.game(this, id, entry.game().definition())
					.setDraggable(offset -> handlers.reorder(id, offset));

			addEntry(listEntry);

			if (listEntry.getId() == selectedId) {
				setSelected(listEntry);
			}
		}
	}

	private Entry createCurrentGameEntry(@Nullable ClientCurrentGame game) {
		Entry entry = new Entry(this, -1)
				.setBanner(true);

		if (game != null) {
			applyRunningGame(game, entry);
		} else {
			applyInactiveGame(entry);
		}

		return entry;
	}

	private void applyRunningGame(ClientCurrentGame game, Entry entry) {
		MutableComponent gameName = game.definition().name().copy().withStyle(ChatFormatting.UNDERLINE);
		entry.setTitle(Component.literal("▶ ").append(gameName));

		if (game.error().isPresent()) {
			entry.setSubtitle(Component.literal("⚠ ").append(game.error().get().copy().withStyle(ChatFormatting.RED)));

			entry.setBackgroundColor(0xFF201010)
					.setHoveredColor(0xFF402020)
					.setSelectedColor(0xFF402020)
					.setOutlineColor(0xFF804040);
		} else {
			entry.setBackgroundColor(0xFF102010)
					.setHoveredColor(0xFF204020)
					.setSelectedColor(0xFF204020)
					.setOutlineColor(0xFF408040);
		}
	}

	private void applyInactiveGame(Entry entry) {
		MutableComponent inactive = GameTexts.Ui.GAME_INACTIVE.copy().withStyle(ChatFormatting.UNDERLINE);
		entry.setTitle(Component.literal("⏸ ").append(inactive));

		entry.setBackgroundColor(0xFF202010)
				.setHoveredColor(0xFF404020)
				.setSelectedColor(0xFF404020)
				.setOutlineColor(0xFF808040);
	}

	private void enqueue(Button button) {
		handlers.enqueue();
	}

	private void remove(Button button) {
		Entry selected = getSelected();
		setSelected(null);

		if (selected != null) {
			removeEntry(selected);
			handlers.remove(selected.getId());
		}
	}

	@Override
	public void setSelected(@Nullable Entry entry) {
		int entryId = entry != null ? entry.getId() : -1;
		handlers.select(entryId);
		removeButton.active = entryId != -1;

		super.setSelected(entry);
	}

	@Override
	public void renderOverlays(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.renderOverlays(graphics, mouseX, mouseY, partialTicks);
		enqueueButton.extractRenderState(graphics, mouseX, mouseY, partialTicks);
		removeButton.extractRenderState(graphics, mouseX, mouseY, partialTicks);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (enqueueButton.mouseClicked(event, doubleClick) || removeButton.mouseClicked(event, doubleClick)) {
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	public interface Handlers {
		void select(int id);

		void enqueue();

		void remove(int id);

		void reorder(int id, int offset);
	}
}
