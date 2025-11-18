package com.lovetropics.minigames.common.core.game.util;

import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;

import java.util.List;

public final class GameBossBar implements GameWidget {
	private static final float UPDATE_PROGRESS_THRESHOLD = 0.001f;

	private final ServerBossEvent bar;

	public GameBossBar(Component title, BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay) {
		bar = new ServerBossEvent(title, color, overlay);
		bar.setDarkenScreen(false);
		bar.setCreateWorldFog(false);
		bar.setPlayBossMusic(false);
	}

	public void setTitle(Component title) {
		bar.setName(title);
	}

	public void setProgress(float progress) {
		if (Math.abs(progress - bar.getProgress()) > UPDATE_PROGRESS_THRESHOLD) {
			bar.setProgress(Mth.clamp(progress, 0.0f, 1.0f));
		}
	}

	public void setStyle(BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay) {
		bar.setColor(color);
		bar.setOverlay(overlay);
	}

	@Override
	public void addPlayer(ServerPlayer player) {
		bar.addPlayer(player);
	}

	@Override
	public void removePlayer(ServerPlayer player) {
		bar.removePlayer(player);
	}

	public void setPlayers(PlayerSet players) {
		for (ServerPlayer oldPlayer : List.copyOf(bar.getPlayers())) {
			if (!players.contains(oldPlayer)) {
				removePlayer(oldPlayer);
			}
		}
		players.forEach(this::addPlayer);
	}

	@Override
	public void close() {
		bar.removeAllPlayers();
		bar.setVisible(false);
	}
}
