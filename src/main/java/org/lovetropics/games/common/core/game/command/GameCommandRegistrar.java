package org.lovetropics.games.common.core.game.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.state.Overlords;

import java.util.function.Predicate;

@FunctionalInterface
public interface GameCommandRegistrar {
	static Predicate<CommandSourceStack> adminOrOverlord(IGamePhase game) {
		Overlords overlords = Overlords.get(game);
		return source -> {
			if (source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
				return true;
			}
			ServerPlayer player = source.getPlayer();
			return player != null && overlords.contains(player);
		};
	}

	default void registerAdmin(String name, SimpleAction action) {
		register(Commands.literal(name)
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(context -> {
					action.execute(context.getSource());
					return 1;
				})
		);
	}

	void register(LiteralArgumentBuilder<CommandSourceStack> subcommand);

	interface SimpleAction {
		void execute(CommandSourceStack source) throws CommandSyntaxException;
	}
}
