package org.lovetropics.games.common.content.mcsr.modifier;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.content.mcsr.McsrTexts;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import org.lovetropics.games.common.core.game.client_state.instance.ModifiersClientState;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.core.game.player.PlayerSet;
import org.lovetropics.games.common.core.game.state.GameStateKey;
import org.lovetropics.games.common.core.game.state.IGameState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/// The modifiers of a game, with their current level. Every player shares the same levels, but each player finds out
/// what the modifiers are on their own: until then, they only get a riddle.
///
/// The modifiers belong to the phase that declares them, and are shared by all phases of the game, so that behaviors in
/// sub-phases (e.g. each player's own world) can read their level.
public final class GameModifiers implements IGameState {
	public static final GameStateKey<GameModifiers> KEY = GameStateKey.create("Modifiers");

	private final IGamePhase game;
	private final Map<String, ModifierDefinition> definitions = new LinkedHashMap<>();
	private final Object2IntMap<String> levels = new Object2IntOpenHashMap<>();
	private final Map<UUID, Set<String>> revealed = new HashMap<>();

	public GameModifiers(IGamePhase game, List<ModifierDefinition> definitions) {
		this.game = game;
		for (ModifierDefinition definition : definitions) {
			this.definitions.put(definition.id(), definition);
			levels.put(definition.id(), definition.baseLevel());
		}
	}

	/// For behaviors that need the modifier to exist, e.g. to read its level
	///
	/// @throws GameException if the game has no modifiers, or not the given one
	public static GameModifiers getOrThrow(IGamePhase game, String id) {
		GameModifiers modifiers = game.instanceState().getOrNull(KEY);
		if (modifiers == null) {
			throw new GameException(Component.literal("Modifier '" + id + "' is used, but the game has no modifiers"));
		}
		validate(game, id);
		return modifiers;
	}

	/// Catches modifiers that don't exist, if the game already has its modifiers
	///
	/// @throws GameException if the modifier doesn't exist
	public static void validate(IGamePhase game, String id) {
		GameModifiers modifiers = game.instanceState().getOrNull(KEY);
		if (modifiers != null && !modifiers.definitions.containsKey(id)) {
			throw new GameException(Component.literal("No modifier with id: " + id));
		}
	}

	public int level(String id) {
		return levels.getInt(id);
	}

	public Set<String> ids() {
		return definitions.keySet();
	}

	public int maxLevel(String id) {
		ModifierDefinition definition = definitions.get(id);
		return definition != null ? definition.maxLevel() : 0;
	}

	/// Changes the level of a modifier for everyone, within the bounds of the modifier
	///
	/// @return whether the level changed
	public boolean change(String id, int amount) {
		return setLevel(id, levels.getInt(id) + amount);
	}

	/// Sets the level of a modifier for everyone, within the bounds of the modifier
	///
	/// @return whether the level changed
	public boolean setLevel(String id, int level) {
		ModifierDefinition definition = definitions.get(id);
		if (definition == null) {
			return false;
		}
		int oldLevel = levels.getInt(id);
		int newLevel = definition.clamp(level);
		if (newLevel == oldLevel) {
			return false;
		}
		levels.put(id, newLevel);

		boolean stronger = newLevel > oldLevel;
		for (ServerPlayer player : game.allPlayers(true)) {
			player.sendSystemMessage(createChangeMessage(player, definition, newLevel, stronger));
		}
		game.allPlayers(true).playSound(stronger ? SoundEvents.BEACON_DEACTIVATE : SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5f, 1.0f);
		updatePlayers();
		return true;
	}

	private Component createChangeMessage(ServerPlayer player, ModifierDefinition definition, int level, boolean stronger) {
		if (isRevealed(player, definition.id())) {
			Component name = definition.name().copy().withStyle(ChatFormatting.AQUA);
			return (stronger ? McsrTexts.MODIFIER_STRONGER : McsrTexts.MODIFIER_WEAKER).apply(name, level, definition.maxLevel()).withStyle(ChatFormatting.LIGHT_PURPLE);
		}
		return (stronger ? McsrTexts.MYSTERY_STRONGER : McsrTexts.MYSTERY_WEAKER).copy().withStyle(ChatFormatting.LIGHT_PURPLE);
	}

	/// Reveals what a modifier is to a player, or a random one they don't know yet if none is given
	///
	/// @return whether there was anything left to reveal
	public boolean reveal(ServerPlayer player, Optional<String> id) {
		Set<String> playerRevealed = revealed.computeIfAbsent(player.getUUID(), _ -> new HashSet<>());
		String revealedId = id.filter(definitions::containsKey).orElseGet(() -> pickHidden(playerRevealed));
		if (revealedId == null || !playerRevealed.add(revealedId)) {
			return false;
		}

		ModifierDefinition definition = definitions.get(revealedId);
		Component name = definition.name().copy().withStyle(ChatFormatting.AQUA);
		player.sendSystemMessage(McsrTexts.MODIFIER_REVEALED.apply(name, definition.description()).withStyle(ChatFormatting.LIGHT_PURPLE));
		PlayerSet.of(player).playSound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.5f);
		updatePlayer(player);
		return true;
	}

	/// Makes a player forget what a modifier is, so that they get its riddle again
	///
	/// @return whether the player knew what the modifier was
	public boolean hide(ServerPlayer player, String id) {
		Set<String> playerRevealed = revealed.get(player.getUUID());
		if (playerRevealed == null || !playerRevealed.remove(id)) {
			return false;
		}
		updatePlayer(player);
		return true;
	}

	private @Nullable String pickHidden(Set<String> playerRevealed) {
		List<String> hidden = definitions.keySet().stream().filter(id -> !playerRevealed.contains(id)).toList();
		return hidden.isEmpty() ? null : hidden.get(game.random().nextInt(hidden.size()));
	}

	private boolean isRevealed(ServerPlayer player, String id) {
		Set<String> playerRevealed = revealed.get(player.getUUID());
		return playerRevealed != null && playerRevealed.contains(id);
	}

	public void updatePlayers() {
		for (ServerPlayer player : game.allPlayers(true)) {
			updatePlayer(player);
		}
	}

	/// Sends the modifiers to the given player, if they are playing - no matter which phase of the game they are in
	public void updatePlayer(ServerPlayer player) {
		if (game.getRoleFor(player) != PlayerRole.PARTICIPANT) {
			return;
		}
		List<ModifiersClientState.Entry> entries = definitions.values().stream()
				.map(definition -> createEntry(player, definition))
				.toList();
		GameClientState.sendToPlayer(new ModifiersClientState(entries), player);
	}

	/// Players only get what a modifier is once they found out, and its riddle until then
	private ModifiersClientState.Entry createEntry(ServerPlayer player, ModifierDefinition definition) {
		int level = levels.getInt(definition.id());
		if (isRevealed(player, definition.id())) {
			return new ModifiersClientState.Entry(definition.icon().create(), Optional.of(definition.name()), Optional.of(definition.description()), definition.riddle(), level, definition.maxLevel());
		}
		return new ModifiersClientState.Entry(ItemStack.EMPTY, Optional.empty(), Optional.empty(), definition.riddle(), level, definition.maxLevel());
	}

	public void removeFromPlayer(ServerPlayer player) {
		GameClientState.removeFromPlayer(GameClientStateTypes.MODIFIERS.get(), player);
	}
}
