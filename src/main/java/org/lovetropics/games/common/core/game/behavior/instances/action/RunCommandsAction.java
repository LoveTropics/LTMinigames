package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.lovetropics.lib.BlockBox;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorType;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorTypes;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.region.RegionQuery;
import org.lovetropics.games.common.core.game.state.DebugModeState;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public record RunCommandsAction(
		Map<RegionQuery, List<String>> regionCommands,
		List<String> entityCommands
) implements IGameBehavior {
	public static final Codec<String> COMMAND_CODEC = Codec.STRING.xmap(
			command -> {
				if (command.startsWith("/")) {
					command = command.substring(1);
				}
				return command;
			},
			Function.identity()
	);

	public static final MapCodec<RunCommandsAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.unboundedMap(RegionQuery.CODEC, ExtraCodecs.compactListCodec(COMMAND_CODEC)).optionalFieldOf("regions", Map.of()).forGetter(RunCommandsAction::regionCommands),
			ExtraCodecs.compactListCodec(COMMAND_CODEC).optionalFieldOf("entity", List.of()).forGetter(RunCommandsAction::entityCommands)
	).apply(i, RunCommandsAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		Commands commands = game.server().getCommands();

		for (Map.Entry<RegionQuery, List<String>> entry : regionCommands.entrySet()) {
			BlockBox box = entry.getKey().one(game);
			CommandSourceStack source = createCommandSourceAtRegion(game, box);

			List<ParseResults<CommandSourceStack>> regionCommands = entry.getValue().stream()
					.map(command -> commands.getDispatcher().parse(command, source))
					.toList();

			for (ParseResults<CommandSourceStack> parse : regionCommands) {
				if (!parse.getExceptions().isEmpty()) {
					CommandSyntaxException exception = parse.getExceptions().values().iterator().next();
					if (exception.getRawMessage() instanceof Component component) {
						throw new GameException(component);
					}
					throw new GameException(Component.literal(exception.getMessage()));
				}
			}

			events.listen(GameActionEvents.APPLY, (context, targets) -> {
				for (ParseResults<CommandSourceStack> command : regionCommands) {
					commands.performCommand(command, command.getReader().getString());
				}
				return true;
			});
		}

		if (!entityCommands.isEmpty()) {
			events.applyToEntities(game, (_, level, entity) -> {
				CommandSourceStack targetSource = createCommandSourceForEntity(game, entity);
				for (String command : entityCommands) {
					commands.performPrefixedCommand(targetSource, command);
				}
				return true;
			});
		}
	}

	private static CommandSourceStack createCommandSourceAtRegion(IGamePhase game, BlockBox region) {
		Component name = game.definition().name();
		return new CommandSourceStack(
				commandSource(game),
				region.center(),
				Vec2.ZERO,
				game.level(),
				LevelBasedPermissionSet.OWNER,
				name.getString(),
				name,
				game.server(),
				null
		);
	}

	private static CommandSourceStack createCommandSourceForEntity(IGamePhase game, Entity entity) {
		return new CommandSourceStack(
				commandSource(game),
				entity.position(),
				entity.getRotationVector(),
				(ServerLevel) entity.level(),
				LevelBasedPermissionSet.OWNER,
				entity.getPlainTextName(),
				entity.getDisplayName(),
				game.server(),
				entity
		);
	}

	private static CommandSource commandSource(IGamePhase game) {
		boolean debugMode = game.state().getOrNull(DebugModeState.KEY) != null;
		return debugMode ? game.server() : CommandSource.NULL;
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.RUN_COMMANDS;
	}
}
