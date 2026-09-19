package com.lovetropics.minigames.common.core.game.rewards;

import com.lovetropics.minigames.common.core.game.state.GameStateKey;
import com.lovetropics.minigames.common.core.game.state.IGameState;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;

public class GameRewardsMap implements IGameState {
	public static final GameStateKey<GameRewardsMap> STATE = GameStateKey.create("Rewards");

	private final Map<UUID, GameRewards> rewards = new Object2ObjectOpenHashMap<>();

	public void clear() {
		rewards.clear();
	}

	public GameRewards forPlayer(ServerPlayer player) {
		return rewards.computeIfAbsent(player.getUUID(), id -> new GameRewards());
	}

	public void grant(ServerPlayer player) {
		GameRewards rewards = this.rewards.remove(player.getUUID());
		if (rewards != null) {
			rewards.grant(player);
		}
	}
}
