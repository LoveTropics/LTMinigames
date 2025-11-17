package com.lovetropics.minigames.common.core.game.behavior.instances.trigger;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.function.Predicate;

public record BindControlsBehavior(Map<Scope, Map<String, GameActionList>> scopedActions) implements IGameBehavior {
	public static final MapCodec<BindControlsBehavior> CODEC = Codec.unboundedMap(Scope.CODEC, Codec.unboundedMap(Codec.STRING, GameActionList.CODEC))
			.xmap(BindControlsBehavior::new, b -> b.scopedActions)
			.fieldOf("scopes");

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		scopedActions.forEach((scope, scopedActions) ->
				scopedActions.values().forEach(actions -> actions.register(game, events))
		);

		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) -> {
			scopedActions.forEach((scope, scopedActions) -> scopedActions.forEach((control, actions) -> {
				commands.register(Commands.literal(control)
						.requires(scope.permissionCheck())
						.executes(context -> {
							Entity entity = context.getSource().getEntity();
							if (entity != null) {
								actions.apply(game, ContextMap.EMPTY, ActionSubjects.ofEntity(entity));
							} else {
								actions.apply(game, ContextMap.EMPTY);
							}
							return 1;
						})
				);
			}));
		});
	}

	public enum Scope implements StringRepresentable {
		EVERYONE("everyone", Commands.hasPermission(Commands.LEVEL_ALL)),
		ADMINS("admins", Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)),
		;

		public static final Codec<Scope> CODEC = StringRepresentable.fromEnum(Scope::values);

		private final String name;
		private final Predicate<CommandSourceStack> permissionCheck;

		Scope(String name, Predicate<CommandSourceStack> permissionCheck) {
			this.name = name;
			this.permissionCheck = permissionCheck;
		}

		public Predicate<CommandSourceStack> permissionCheck() {
			return permissionCheck;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}
}
