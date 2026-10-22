package org.lovetropics.games.common.content.mcsr.modifier;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.behavior.event.SubGameEvents;
import org.lovetropics.games.common.core.game.state.GameStateMap;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Sets up the [GameModifiers] of this game, which behaviors in this phase and in any of its sub-phases can read
public final class ModifiersBehavior implements IGameBehavior {
	public static final MapCodec<ModifiersBehavior> CODEC = ModifierDefinition.CODEC.listOf().fieldOf("modifiers")
			.validate(ModifiersBehavior::validateUniqueIds)
			.xmap(ModifiersBehavior::new, b -> b.modifiers);

	private final List<ModifierDefinition> modifiers;

	private GameModifiers state;

	public ModifiersBehavior(List<ModifierDefinition> modifiers) {
		this.modifiers = modifiers;
	}

	private static DataResult<List<ModifierDefinition>> validateUniqueIds(List<ModifierDefinition> modifiers) {
		Set<String> ids = new HashSet<>();
		for (ModifierDefinition modifier : modifiers) {
			if (!ids.add(modifier.id())) {
				return DataResult.error(() -> "Duplicate modifier id: " + modifier.id());
			}
		}
		return DataResult.success(modifiers);
	}

	@Override
	public void registerState(IGamePhase game, GameStateMap phaseState, GameStateMap instanceState) {
		state = instanceState.register(GameModifiers.KEY, new GameModifiers(game, modifiers));
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GamePlayerEvents.ADD, state::updatePlayer);
		events.listen(GamePlayerEvents.REMOVE, state::removeFromPlayer);
		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) -> ModifierCommands.register(game, state, commands));

		// Players moving into a sub-phase are removed from this one, which clears their modifiers - send them again once they arrive
		events.listen(SubGameEvents.CREATE, (subGame, subEvents) -> {
			subEvents.listen(GamePlayerEvents.ADD, state::updatePlayer);
			subEvents.listen(GamePlayerEvents.REMOVE, state::removeFromPlayer);
		});
	}
}
