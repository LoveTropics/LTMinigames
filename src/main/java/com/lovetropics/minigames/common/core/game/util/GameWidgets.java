package com.lovetropics.minigames.common.core.game.util;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.state.GameStateKey;
import com.lovetropics.minigames.common.core.game.state.IGameState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;

import java.util.ArrayList;
import java.util.List;

public final class GameWidgets implements IGameState {
	public static final GameStateKey<GameWidgets> KEY = GameStateKey.create("GameWidgets");

	private final IGamePhase game;
	private final List<GameWidget> allWidgets = new ArrayList<>();
	private final List<GameWidget> globalWidgets = new ArrayList<>();

	private GameWidgets(IGamePhase game) {
		this.game = game;
	}

	public static GameWidgets getOrRegister(IGamePhase game, EventRegistrar events) {
		GameWidgets widgets = game.state().getOrNull(KEY);
		if (widgets != null) {
			return widgets;
		}
		widgets = new GameWidgets(game);
		widgets.registerEventListeners(events);
		game.state().register(KEY, widgets);
		return widgets;
	}

	private void registerEventListeners(EventRegistrar events) {
		events.listen(GamePlayerEvents.ADD, player -> {
			for (GameWidget widget : globalWidgets) {
				widget.addPlayer(player);
			}
		});
		events.listen(GamePlayerEvents.REMOVE, player -> {
			for (GameWidget widget : allWidgets) {
				widget.removePlayer(player);
			}
		});
		events.listen(GamePhaseEvents.DESTROY, () -> {
			for (GameWidget widget : allWidgets) {
				widget.close();
			}
			allWidgets.clear();
			globalWidgets.clear();
		});
	}

	public GameSidebar openSidebar(Component title) {
		return registerWidget(new GameSidebar(this, game.server(), title), false);
	}

	public GameBossBar openBossBar(Component title, BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay) {
		return registerWidget(new GameBossBar(this, title, color, overlay), false);
	}

	public GameSidebar openGlobalSidebar(Component title) {
		return registerWidget(new GameSidebar(this, game.server(), title), true);
	}

	public GameBossBar openGlobalBossBar(Component title, BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay) {
		return registerWidget(new GameBossBar(this, title, color, overlay), true);
	}

	public <T extends GameWidget> T registerWidget(T widget, boolean global) {
		allWidgets.add(widget);
		if (global) {
			globalWidgets.add(widget);
			game.allPlayers().forEach(widget::addPlayer);
		}
		return widget;
	}

	/* package-private */ void remove(GameWidget widget) {
		allWidgets.remove(widget);
		globalWidgets.remove(widget);
	}
}
