package com.lovetropics.minigames.common.core.game.lobby;

import com.lovetropics.minigames.common.core.game.GameResult;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.player.PlayerRoleSelections;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.util.TeamAllocator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public interface IGameLobbyPlayers extends PlayerSet {
	TeamAllocator<PlayerRole, ServerPlayer> createRoleAllocator();

	CompletableFuture<GameResult<Unit>> joinAndPrompt(ServerPlayer player);

	GameResult<Unit> join(ServerPlayer player, PlayerRole role);

	boolean remove(ServerPlayer player, boolean loggingOut);

	boolean forceRole(ServerPlayer player, @Nullable PlayerRole role);

	PlayerRoleSelections getRoleSelections();
}
