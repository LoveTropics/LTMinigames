package org.lovetropics.games.common.core.command;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.chat.ChatChannel;
import org.lovetropics.games.common.core.chat.ChatChannelStore;
import org.lovetropics.games.common.core.command.argument.ChatChannelArgument;
import org.lovetropics.games.common.core.game.util.GameTexts;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@EventBusSubscriber(modid = LoveTropics.ID)
public class ChatCommand {
	public static void register(RegisterCommandsEvent event) {
		CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
		dispatcher.register(literal("chat")
				.then(ChatChannelArgument.argument("channel")
						.executes(context -> {
							ServerPlayer player = context.getSource().getPlayerOrException();
							ChatChannel channel = ChatChannelArgument.get(context, "channel");
							ChatChannelStore.set(player, channel);
							context.getSource().sendSuccess(() -> GameTexts.Commands.SET_CHAT_CHANNEL.apply(channel.getName()), false);
							return 1;
						})
				)
		);
		dispatcher.register(literal("shout")
				.then(argument("message", MessageArgument.message())
						.executes(context -> {
							CommandSourceStack source = context.getSource();
							ServerPlayer player = source.getPlayerOrException();
							MessageArgument.resolveChatMessage(context, "message", message -> {
								MinecraftServer server = source.getServer();
								server.getPlayerList().broadcastChatMessage(message, player, ChatType.bind(ChatType.CHAT, player));
							});
							return 1;
						})
				)
		);
	}
}
