package org.lovetropics.games.common.core.game.behavior.instances.donation;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjectType;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.behavior.action.GameActionContextKeys;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePackageEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import org.lovetropics.games.common.core.game.state.GamePackageState;
import org.lovetropics.games.common.core.game.state.team.TeamState;
import org.lovetropics.games.common.core.integration.game_actions.GamePackage;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/// @param includeSubPhases whether the package can also go to participants who are in sub-phases of this phase, e.g.
///                         each playing in their own world
public record DonationPackageBehavior(
		DonationPackageData data,
		Optional<DonationPackageNotification> notification,
		GameActionList receiveActions,
		// TODO: We shouldn't really rely on triggering the package in the top-level game, behaviors (correctly) make the assumption that the target of an action exists within that game instance
		//  MCSR should probably be creating levels at runtime rather than sub-games
		boolean includeSubPhases
) implements IGameBehavior {
	private static final Logger LOGGER = LogUtils.getLogger();

	public static final MapCodec<DonationPackageBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			DonationPackageData.CODEC.forGetter(c -> c.data),
			DonationPackageNotification.CODEC.optionalFieldOf("notification").forGetter(c -> c.notification),
			GameActionList.CODEC.optionalFieldOf("receive_actions", GameActionList.EMPTY).forGetter(c -> c.receiveActions),
			Codec.BOOL.optionalFieldOf("include_sub_phases", false).forGetter(c -> c.includeSubPhases)
	).apply(i, DonationPackageBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, _) -> {
			LiteralArgumentBuilder<CommandSourceStack> subcommand = Commands.literal(data.id())
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
			// The amount is optional, to simulate a donation of a given amount
			switch (data.targetSelectionMode()) {
				case SPECIFIC -> subcommand.then(Commands.argument("target", EntityArgument.players())
						.executes(ctx -> spawnPackageFromCommand(game, ctx, EntityArgument.getPlayers(ctx, "target"), Optional.empty()))
						.then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
								.executes(ctx -> spawnPackageFromCommand(game, ctx, EntityArgument.getPlayers(ctx, "target"), Optional.of(DoubleArgumentType.getDouble(ctx, "amount"))))
						));
				case RANDOM, ALL -> subcommand.executes(ctx -> spawnPackageFromCommand(game, ctx, null, Optional.empty()))
						.then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
								.executes(ctx -> spawnPackageFromCommand(game, ctx, null, Optional.of(DoubleArgumentType.getDouble(ctx, "amount"))))
						);
			}
			commands.register(Commands.literal("package").then(subcommand));
		});

		events.listen(GamePackageEvents.RECEIVE_PACKAGE, gamePackage -> onGamePackageReceived(game, gamePackage));

		receiveActions.register(game, events);

		PackageCostModifierBehavior.State costModifier = game.state().get(PackageCostModifierBehavior.State.KEY);
		game.state().get(GamePackageState.KEY).addPackageType(data.apply(costModifier));
	}

	private int spawnPackageFromCommand(IGamePhase game, CommandContext<CommandSourceStack> ctx, @Nullable Collection<ServerPlayer> targetPlayers, Optional<Double> amount) {
		GamePackage gamePackage = new GamePackage(
				data.id(),
				"LoveTropics",
				targetPlayers != null ? ActionSubjects.ofPlayers(List.copyOf(targetPlayers)) : null,
				amount
		);
		switch (onGamePackageReceived(game, gamePackage)) {
			case TRUE -> ctx.getSource().sendSuccess(() -> Component.translatable("Successfully sent '%s'", data.id()), true);
			case DEFAULT -> ctx.getSource().sendFailure(Component.translatable("'%s' was not processed", data.id()));
			case FALSE -> ctx.getSource().sendFailure(Component.translatable("'%s' was rejected", data.id()));
		}
		return Command.SINGLE_SUCCESS;
	}

	private TriState onGamePackageReceived(IGamePhase game, GamePackage gamePackage) {
		if (!gamePackage.packageType().equals(data.id())) {
			return TriState.DEFAULT;
		}

		ActionSubjects<?> targets = resolveTargets(game, gamePackage);
		if (targets == null) {
			LOGGER.warn("Expected donation package to have a receiver, but did not resolve any for {}", gamePackage);
			return TriState.FALSE;
		}

		ContextMap context = actionContext(gamePackage);
		if (receiveActions.apply(game, context, targets)) {
			notification.ifPresent(notification ->
					notification.broadcast(game, targets, gamePackage.sendingPlayerName(), data.name())
			);
			return TriState.TRUE;
		}
		return TriState.FALSE;
	}

	private ActionSubjects<?> narrowTargetsForGame(IGamePhase game, ActionSubjects<?> globalTargets) {
		if (includeSubPhases) {
			return globalTargets;
		}
		return globalTargets.filterPlayers(game, player -> game.allPlayers().contains(player));
	}

	private @Nullable ActionSubjects<?> resolveTargets(IGamePhase game, GamePackage gamePackage) {
		// If we got a request for specific targets, just do that regardless of our configuration - always trust in the tech stack :)
		ActionSubjects<?> specificTargets = gamePackage.specificTargets();
		if (specificTargets != null) {
			return narrowTargetsForGame(game, specificTargets);
		}
		ActionSubjects<?> allTargets = resolveAllTargets(game);
		return switch (data.targetSelectionMode()) {
			case SPECIFIC -> null;
			case RANDOM -> {
				if (data.applyToTeam()) {
					yield allTargets.takeOneRandom(game, ActionSubjectType.TEAM);
				} else {
					yield allTargets.takeOneRandom(game, ActionSubjectType.PLAYER);
				}
			}
			case ALL -> allTargets;
		};
	}

	private ActionSubjects<?> resolveAllTargets(IGamePhase game) {
		if (data.applyToTeam()) {
			TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
			return ActionSubjects.ofTeams(teams != null ? List.copyOf(teams.getTeamKeys()) : List.of());
		} else {
			// Players in sub-phases are no longer in this phase, but keep their role here
			return ActionSubjects.ofPlayers(
					game.allPlayers(includeSubPhases).filter(player -> game.getRoleFor(player) == PlayerRole.PARTICIPANT)
			);
		}
	}

	private static ContextMap actionContext(GamePackage gamePackage) {
		ContextMap.Builder context = new ContextMap.Builder();
		context.withParameter(GameActionContextKeys.PACKAGE, gamePackage);
		if (gamePackage.sendingPlayerName() != null) {
			context.withParameter(GameActionContextKeys.PACKAGE_SENDER, gamePackage.sendingPlayerName());
		}
		return context.create(ContextKeySet.EMPTY);
	}
}
