package org.lovetropics.games.common.core.command.game;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.util.Util;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.game.IGameLookup;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.util.GameTexts;
import org.lovetropics.games.common.core.integration.game_actions.Donation;
import org.lovetropics.games.common.core.integration.game_actions.DonationGameAction;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/// `/game donation <amount>` sends the game a donation as if it came from the tech stack for lazy people that don't want to open their browser on staging
/// TODO add a way to select a player if game supports
@EventBusSubscriber(modid = LoveTropics.ID)
public class FakeGameDonationCommand {
	@SubscribeEvent
	public static void register(RegisterCommandsEvent event) {
		event.getDispatcher().register(literal("game")
				.then(literal("donation").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(argument("amount", DoubleArgumentType.doubleArg(0.0))
								.executes(FakeGameDonationCommand::donate)
						)
				)
		);
	}

	private static int donate(CommandContext<CommandSourceStack> context) {
		IGamePhase game = IGameLookup.get().getGamePhaseFor(context.getSource());
		if (game == null) {
			context.getSource().sendFailure(GameTexts.Commands.NOT_IN_GAME);
			return 0;
		}
		double amount = DoubleArgumentType.getDouble(context, "amount");
		Donation donation = new Donation(context.getSource().getTextName(), amount, "", false, 0.0, "", Util.NIL_UUID);
		new DonationGameAction(donation).apply(game);
		context.getSource().sendSuccess(() -> GameTexts.Commands.FAKE_DONATION.apply(amount), true);
		return Command.SINGLE_SUCCESS;
	}
}
