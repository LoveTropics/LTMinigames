package com.lovetropics.minigames.common.core.game.datagen;

import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionTarget;
import com.lovetropics.minigames.common.core.game.behavior.action.ApplyToBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.action.PlayerActionTarget;
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

	public <T> GameActionList<T> applyToAllPlayers(ActionTarget<T> target, IGameBehavior... behaviors) {
		return new GameActionList<>(applyToAllPlayersBehavior(behaviors), target);
	}

	public <T> IGameBehavior applyToAllPlayersBehavior(IGameBehavior... behaviors) {
		return new ApplyToBehavior<>(new PlayerActionTarget(PlayerActionTarget.Target.ALL),
				new GameActionList<>(list(behaviors),
						new PlayerActionTarget(PlayerActionTarget.Target.SOURCE)), GameBehaviorTypes.APPLY_TO_PLAYER);
	}

	public <T> GameActionList<T> actions(ActionTarget<T> target, IGameBehavior behaviors) {
		return new GameActionList<>(list(behaviors), target);
	}

	public IGameBehavior list(IGameBehavior... behaviors) {
		return behaviors.length == 1 ? behaviors[0] : new CompositeBehavior(List.of(behaviors));
	}
}
