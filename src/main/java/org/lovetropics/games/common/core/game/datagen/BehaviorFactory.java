package org.lovetropics.games.common.core.game.datagen;

import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.ActionTarget;
import org.lovetropics.games.common.core.game.behavior.action.ApplyToAction;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.instances.CompositeBehavior;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class BehaviorFactory {
	private final Map<Identifier, IGameBehavior> behaviors = new HashMap<>();

	public IGameBehavior direct(Identifier name, IGameBehavior behavior) {
		behaviors.put(name, behavior);
		return new DirectBehavior(name, behavior);
	}

	public Stream<Map.Entry<Identifier, IGameBehavior>> stream() {
		return behaviors.entrySet().stream();
	}

	public GameActionList applyToAllPlayers(IGameBehavior... behaviors) {
		return new GameActionList(list(behaviors), ActionTarget.Simple.ALL_PLAYERS, 1);
	}

	public IGameBehavior applyToAllPlayersBehavior(IGameBehavior... behaviors) {
		return new ApplyToAction(applyToAllPlayers(behaviors));
	}

	public GameActionList actions(ActionTarget target, IGameBehavior behaviors) {
		return new GameActionList(list(behaviors), target, 1);
	}

	public IGameBehavior list(IGameBehavior... behaviors) {
		return behaviors.length == 1 ? behaviors[0] : new CompositeBehavior(List.of(behaviors));
	}
}
