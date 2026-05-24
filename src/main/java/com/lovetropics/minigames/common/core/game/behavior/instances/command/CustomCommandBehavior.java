package com.lovetropics.minigames.common.core.game.behavior.instances.command;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.command.GameCommandRegistrar;
import com.lovetropics.minigames.common.core.game.state.Overlords;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.context.ContextMap;

import java.util.Collection;
import java.util.List;

public record CustomCommandBehavior(
		List<String> path,
		PermissionLevel permissionLevel,
		Argument argument,
		GameActionList actions
) implements IGameBehavior {
	public static final MapCodec<CustomCommandBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ExtraCodecs.nonEmptyList(ExtraCodecs.RESOURCE_PATH_CODEC.listOf()).fieldOf("path").forGetter(CustomCommandBehavior::path),
			PermissionLevel.CODEC.optionalFieldOf("permission_level", PermissionLevel.ADMIN).forGetter(CustomCommandBehavior::permissionLevel),
			Argument.CODEC.optionalFieldOf("argument", Argument.NONE).forGetter(CustomCommandBehavior::argument),
			GameActionList.MAP_CODEC.forGetter(CustomCommandBehavior::actions)
	).apply(i, CustomCommandBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		actions.register(game, events);
		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) ->
				registerCommands(game, commands)
		);
	}

	private void registerCommands(IGamePhase game, GameCommandRegistrar commands) {
		LiteralArgumentBuilder<CommandSourceStack> tail = Commands.literal(path.getLast());
		tail.requires(source -> permissionLevel.hasPermission(game, source));

		switch (argument) {
			case NONE -> tail.executes(context -> {
				ServerPlayer player = context.getSource().getPlayer();
				ActionSubjects<?> source = player != null ? ActionSubjects.ofPlayer(player) : ActionSubjects.EMPTY;
				return actions.apply(game, ContextMap.EMPTY, source) ? 1 : 0;
			});
			case PLAYER -> tail.then(Commands.argument("player", EntityArgument.players())
					.executes(context -> {
						Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "player");
						return actions.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayers(List.copyOf(players))) ? 1 : 0;
					})
			);
		}

		LiteralArgumentBuilder<CommandSourceStack> head = tail;
		for (int i = path.size() - 2; i >= 0; i--) {
			head = Commands.literal(path.get(i)).then(head);
		}
		commands.register(head);
	}

	public enum PermissionLevel implements StringRepresentable {
		ADMIN("admin"),
		OVERLORD("overlord"),
		EVERYONE("everyone"),
		;

		public static final Codec<PermissionLevel> CODEC = StringRepresentable.fromEnum(PermissionLevel::values);

		private final String name;

		PermissionLevel(String name) {
			this.name = name;
		}

		public boolean hasPermission(IGamePhase game, CommandSourceStack source) {
			if (source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
				return true;
			}
			ServerPlayer player = source.getPlayer();
			if (this == OVERLORD && player != null && Overlords.get(game).contains(player)) {
				return true;
			}
			return this == EVERYONE;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}

	public enum Argument implements StringRepresentable {
		NONE("none"),
		PLAYER("player"),
		// TODO Team
		;

		public static final Codec<Argument> CODEC = StringRepresentable.fromEnum(Argument::values);

		private final String name;

		Argument(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}
}
