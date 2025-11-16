package com.lovetropics.minigames.common.core.game.datagen;

import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionTarget;
import com.lovetropics.minigames.common.core.game.behavior.action.ApplyToAction;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.instances.CompositeBehavior;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class BehaviorFactory {
	private final Map<ResourceLocation, IGameBehavior> behaviors = new HashMap<>();

	public IGameBehavior direct(ResourceLocation name, IGameBehavior behavior) {
		behaviors.put(name, behavior);
		return new DirectBehavior(name, behavior);
	}

	public Stream<Map.Entry<ResourceLocation, IGameBehavior>> stream() {
		return behaviors.entrySet().stream();
	}

	public GameActionList applyToAllPlayers(IGameBehavior... behaviors) {
		return new GameActionList(list(behaviors), ActionTarget.Simple.ALL_PLAYERS);
	}

	public IGameBehavior applyToAllPlayersBehavior(IGameBehavior... behaviors) {
		return new ApplyToAction(applyToAllPlayers(behaviors));
	}

	public GameActionList actions(ActionTarget target, IGameBehavior behaviors) {
		return new GameActionList(list(behaviors), target);
	}

	public IGameBehavior list(IGameBehavior... behaviors) {
		return behaviors.length == 1 ? behaviors[0] : new CompositeBehavior(List.of(behaviors));
	}
}
