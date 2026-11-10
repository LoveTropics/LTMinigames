package org.lovetropics.games.lobbies.client.manage.screen.game_list;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lovetropics.games.lobbies.GameLobbyTexts;
import org.lovetropics.games.lobbies.client.ClientGameDefinition;
import org.lovetropics.games.lobbies.client.manage.state.ClientLobbyManageState;
import org.lovetropics.games.lobbies.client.screen.FlexUi;
import org.lovetropics.games.lobbies.client.screen.flex.Flex;
import org.lovetropics.games.lobbies.client.screen.flex.FlexSolver;
import org.lovetropics.games.lobbies.client.screen.flex.Layout;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.IntConsumer;

public final class InstalledGameList extends AbstractGameList {
	private static final Component TITLE = GameLobbyTexts.Ui.INSTALLED_GAMES.copy()
			.withStyle(ChatFormatting.UNDERLINE, ChatFormatting.BOLD);

	private final ClientLobbyManageState lobby;
	private final IntConsumer enqueue;
	private final Runnable back;

	private final Button backButton;
	private final EditBox searchField;

	public InstalledGameList(Screen screen, Layout main, Layout footer, ClientLobbyManageState lobby, IntConsumer enqueue, Runnable back) {
		super(screen, main, TITLE);
		this.lobby = lobby;
		this.enqueue = enqueue;
		this.back = back;

		Flex root = new Flex().row();
		Flex backFlex = root.child().size(20, 20).marginRight(2);
		Flex search = root.child().grow(1.0F).height(20).marginLeft(2);

		FlexSolver.Results solve = new FlexSolver(footer.content()).apply(root);
		backButton = FlexUi.createButton(solve.layout(backFlex), Component.literal("←"), button -> back.run());
		backButton.setTooltip(Tooltip.create(GameLobbyTexts.Ui.BACK_TO_QUEUE));
		searchField = FlexUi.createTextField(solve.layout(search), screen.getMinecraft().font, GameLobbyTexts.Ui.SEARCH_GAMES);
		searchField.setHint(GameLobbyTexts.Ui.SEARCH_GAMES);
		searchField.setResponder(query -> updateEntries());
	}

	@Override
	public Optional<GuiEventListener> getChildAt(double x, double y) {
		if (backButton.isMouseOver(x, y)) {
			return Optional.of(backButton);
		}
		return super.getChildAt(x, y);
	}

	@Override
	public Optional<GuiEventListener> getWidgetAt(double x, double y) {
		if (searchField.isMouseOver(x, y)) {
			return Optional.of(searchField);
		}
		return Optional.empty();
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return backButton.isMouseOver(mouseX, mouseY) || super.isMouseOver(mouseX, mouseY);
	}

	@Override
	public void updateEntries() {
		setSelected(null);

		List<ClientGameDefinition> games = lobby.getInstalledGames();
		String query = searchField.getValue().trim().toLowerCase(Locale.ROOT);

		clearEntries();
		setScrollAmount(0.0);
		for (int id = 0; id < games.size(); id++) {
			ClientGameDefinition game = games.get(id);
			if (!matches(game, query)) {
				continue;
			}
			addEntry(createEntry(id, game));
		}
	}

	private Entry createEntry(int id, ClientGameDefinition game) {
		Entry entry = Entry.game(this, id, game);
		entry.addAction(Component.literal("✔"), GameLobbyTexts.Ui.ADD_TO_QUEUE, () -> enqueue.accept(id));
		entry.setActivateAction(() -> enqueue.accept(id));
		return entry;
	}

	private static boolean matches(ClientGameDefinition game, String query) {
		if (query.isEmpty()) {
			return true;
		}
		return game.name().getString().toLowerCase(Locale.ROOT).contains(query)
				|| game.id().toString().contains(query);
	}

	@Override
	public void renderOverlays(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.renderOverlays(graphics, mouseX, mouseY, partialTicks);
		backButton.extractRenderState(graphics, mouseX, mouseY, partialTicks);
		searchField.extractRenderState(graphics, mouseX, mouseY, partialTicks);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (backButton.mouseClicked(event, doubleClick)) {
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}
}
