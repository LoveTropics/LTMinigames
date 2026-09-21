package org.lovetropics.games.common.content.escape_race.ddr_machine;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.JukeboxSong;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.lovetropics.games.LoveTropics;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@EventBusSubscriber(modid = LoveTropics.ID)
public final class DDRCommand {

	private static final SimpleCommandExceptionType NOT_DDRING = new SimpleCommandExceptionType(Component.literal("You are not playing DDR!"));

	@SubscribeEvent
	public static void register(RegisterCommandsEvent event) {
		// @formatter:off
        event.getDispatcher().register(
            literal("ddr")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.requires(CommandSourceStack::isPlayer)
					.then(literal("level")
							.then(literal("record")
									.then(literal("stop")
											.executes(DDRCommand::stopRecording))
									.then(argument("track", ResourceArgument.resource(event.getBuildContext(), Registries.JUKEBOX_SONG))
											.then(argument("name", StringArgumentType.string())
													.executes(DDRCommand::startRecording))))
					)
        );
        // @formatter:on
	}
	private static int startRecording(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		Holder<JukeboxSong> track = ResourceArgument.getResource(context, "track", Registries.JUKEBOX_SONG);
		String name = StringArgumentType.getString(context, "name");
		ServerPlayer player = context.getSource().getPlayerOrException();
		if (player.getControlledVehicle() instanceof DDRMachineEntity ddrMachineEntity) {
			ddrMachineEntity.startRecording(player, track, name);
			return Command.SINGLE_SUCCESS;
		}
		throw NOT_DDRING.create();
	}

	private static int stopRecording(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		if (player.getControlledVehicle() instanceof DDRMachineEntity ddrMachineEntity) {
			ddrMachineEntity.stopRecording(player);
			return Command.SINGLE_SUCCESS;
		}
		throw NOT_DDRING.create();
	}

}
