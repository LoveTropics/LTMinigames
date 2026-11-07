package org.lovetropics.games.common.core.game.behavior.instances;

import com.mojang.serialization.MapCodec;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.core.game.state.Ghosts;
import org.lovetropics.games.common.core.game.state.Overlords;

/// Turns participants who become spectators into [Ghosts]. Other spectators only become ghosts through the command.
public final class GhostsBehavior implements IGameBehavior {
	public static final MapCodec<GhostsBehavior> CODEC = MapCodec.unit(GhostsBehavior::new);

	private static final String TAG = "ghost";

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		Ghosts ghosts = Ghosts.get(game);
		Overlords overlords = Overlords.get(game);

		events.listen(GamePlayerEvents.SET_ROLE, (player, role, lastRole) -> {
			if (lastRole == PlayerRole.PARTICIPANT && role == PlayerRole.SPECTATOR && !overlords.contains(player)) {
				ghosts.add(player);
				player.addTag(TAG);
			}
		});

		events.listen(GamePlayerEvents.ADD, player -> {
			if (ghosts.contains(player)) {
				player.addTag(TAG);
			}
		});

		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) -> {
			commands.register(Commands.literal("ghost")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.then(Commands.literal("add")
							.then(Commands.argument("players", EntityArgument.players())
									.executes(context -> {
										EntityArgument.getPlayers(context, "players").forEach(ghosts::add);
										return 1;
									})
							)
					)
					.then(Commands.literal("remove")
							.then(Commands.argument("players", EntityArgument.players())
									.executes(context -> {
										EntityArgument.getPlayers(context, "players").forEach(ghosts::remove);
										return 1;
									})
							)
					)
			);
		});
	}
}
