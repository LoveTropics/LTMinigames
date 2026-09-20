package org.lovetropics.games.common.core.game.behavior.action;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.game.state.team.GameTeam;
import org.lovetropics.games.common.core.integration.game_actions.GamePackage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public class GameActionContextKeys<T> {
	public static final ContextKey<GamePackage> PACKAGE = create("package");
	public static final ContextKey<String> PACKAGE_SENDER = create("package_sender");

	public static final ContextKey<ServerPlayer> KILLER = create("killer");
	public static final ContextKey<ServerPlayer> KILLED = create("killed");
	public static final ContextKey<Entity> TARGET = create("target");
	public static final ContextKey<ServerPlayer> SCORER = create("scorer");
	public static final ContextKey<Integer> COUNT = create("count");
	public static final ContextKey<ItemStack> ITEM = create("item");
	public static final ContextKey<GameTeam> TEAM = create("team");
	public static final ContextKey<Component> NAME = create("name");
	public static final ContextKey<Component> WINNER = create("winner");
	public static final ContextKey<Integer> CHANGE = create("change");

	private GameActionContextKeys() {
	}

	private static <T> ContextKey<T> create(String name) {
		return new ContextKey<>(LoveTropics.id(name));
	}
}
