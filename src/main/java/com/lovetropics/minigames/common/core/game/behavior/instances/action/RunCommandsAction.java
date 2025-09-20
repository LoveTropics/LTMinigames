package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.state.DebugModeState;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public record RunCommandsAction(List<String> globalCommands, List<String> playerCommands) implements IGameBehavior {
    private static final Logger LOGGER = LogManager.getLogger(RunCommandsAction.class);

    private static final Codec<String> COMMAND_CODEC = Codec.STRING.xmap(
            command -> {
                if (command.startsWith("/")) {
                    command = command.substring(1);
                }
                return command;
            },
            Function.identity()
    );

    public static final MapCodec<RunCommandsAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ExtraCodecs.compactListCodec(COMMAND_CODEC).optionalFieldOf("global", List.of()).forGetter(RunCommandsAction::globalCommands),
            ExtraCodecs.compactListCodec(COMMAND_CODEC).optionalFieldOf("player", List.of()).forGetter(RunCommandsAction::playerCommands)
    ).apply(i, RunCommandsAction::new));

    @Override
    public void register(IGamePhase game, EventRegistrar events) {
        Commands commands = game.server().getCommands();
        CommandSourceStack source = createCommandSource(game);

        if (!globalCommands.isEmpty()) {
            List<ParseResults<CommandSourceStack>> globalCommands = this.globalCommands.stream()
                    .map(command -> commands.getDispatcher().parse(command, source))
                    .toList();

            for (ParseResults<CommandSourceStack> parse : globalCommands) {
                if (!parse.getExceptions().isEmpty()) {
                    CommandSyntaxException exception = parse.getExceptions().values().iterator().next();
                    if (exception.getRawMessage() instanceof Component component) {
                        throw new GameException(component);
                    }
                    throw new GameException(Component.literal(exception.getMessage()));
                }
            }

            events.listen(GameActionEvents.APPLY, context -> {
                for (ParseResults<CommandSourceStack> command : globalCommands) {
                    commands.performCommand(command, command.getReader().getString());
                }
                return true;
            });
        }

        if (!playerCommands.isEmpty()) {
            events.listen(GameActionEvents.APPLY_TO_PLAYER, (context, target) -> {
                CommandSourceStack targetSource = source.withEntity(target).withPosition(target.position());
                for (String command : playerCommands) {
                    commands.performPrefixedCommand(targetSource, command);
                }
                return true;
            });
        }
    }

    private static CommandSourceStack createCommandSource(IGamePhase game) {
        boolean debugMode = game.state().getOrNull(DebugModeState.KEY) != null;
        CommandSource source = debugMode ? game.server() : CommandSource.NULL;

        String name = game.lobby().getMetadata().name();
        return new CommandSourceStack(source, Vec3.ZERO, Vec2.ZERO, game.level(), Commands.LEVEL_OWNERS, name, Component.literal(name), game.server(), null);
    }

    @Override
    public Supplier<? extends GameBehaviorType<?>> behaviorType() {
        return GameBehaviorTypes.RUN_COMMANDS;
    }
}
