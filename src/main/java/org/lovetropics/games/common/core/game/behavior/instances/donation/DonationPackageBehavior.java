package org.lovetropics.games.common.core.game.behavior.instances.donation;

import com.lovetropics.lib.codec.MoreCodecs;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
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
import org.lovetropics.games.common.core.integration.game_actions.GamePackage;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/// @param includeSubPhases whether the package can also go to participants who are in sub-phases of this phase, e.g.
///                         each playing in their own world
public record DonationPackageBehavior(
		String id,
		DonationPackageDesc.Type type,
		Optional<Component> nameOverride,
		Optional<Component> descriptionOverride,
		TargetSelectionMode targetMode,
		Optional<Double> donationAmount,
		GameActionList receiveActions,
		// TODO: We shouldn't really rely on triggering the package in the top-level game, behaviors (correctly) make the assumption that the target of an action exists within that game instance
		//  MCSR should probably be creating levels at runtime rather than sub-games
		boolean includeSubPhases
) implements IGameBehavior {
	private static final Logger LOGGER = LogUtils.getLogger();

	public static final MapCodec<DonationPackageBehavior> CODEC = RecordCodecBuilder.<DonationPackageBehavior>mapCodec(i -> i.group(
			Codec.STRING.fieldOf("id").forGetter(DonationPackageBehavior::id),
			DonationPackageDesc.Type.CODEC.fieldOf("package_type").forGetter(DonationPackageBehavior::type),
			ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(DonationPackageBehavior::nameOverride),
			ComponentSerialization.CODEC.optionalFieldOf("description").forGetter(DonationPackageBehavior::descriptionOverride),
			TargetSelectionMode.CODEC.fieldOf("target").forGetter(DonationPackageBehavior::targetMode),
			Codec.DOUBLE.optionalFieldOf("donation_amount").forGetter(DonationPackageBehavior::donationAmount),
			GameActionList.CODEC.fieldOf("receive_actions").forGetter(DonationPackageBehavior::receiveActions),
			Codec.BOOL.optionalFieldOf("include_sub_phases", false).forGetter(DonationPackageBehavior::includeSubPhases)
	).apply(i, DonationPackageBehavior::new)).validate(DonationPackageBehavior::validate);

	private DataResult<DonationPackageBehavior> validate() {
		if (type == DonationPackageDesc.Type.AUTO && targetMode.requiresSelection()) {
			return DataResult.error(() -> "Auto package " + id + " with type " + type + " cannot use specific select mode");
		}
		return DataResult.success(this);
	}

	private Component name() {
		return nameOverride.orElseGet(() -> Component.translatable("ltminigames.minigame.donation." + id));
	}

	private Component description() {
		return descriptionOverride.orElseGet(() -> Component.translatable("ltminigames.minigame.donation." + id + ".description"));
	}

	private DonationPackageDesc makePackageDesc(PackageCostModifierBehavior.State costModifier) {
		Component name = name();
		Component description = description();
		return new DonationPackageDesc(
				id,
				type,
				targetMode.toExternal(),
				name,
				name.getString(),
				description,
				description.getString(),
				donationAmount.map(costModifier::apply)
		);
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		PackageCostModifierBehavior.State costModifier = game.state().get(PackageCostModifierBehavior.State.KEY);
		DonationPackageDesc packageDesc = makePackageDesc(costModifier);

		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, _) -> {
			LiteralArgumentBuilder<CommandSourceStack> subcommand = Commands.literal(id)
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
			// The amount is optional, to simulate a donation of a given amount
			switch (targetMode) {
				case PLAYER, TEAM -> subcommand.then(Commands.argument("target", EntityArgument.players())
						.executes(ctx -> spawnPackageFromCommand(game, ctx, EntityArgument.getPlayers(ctx, "target"), Optional.empty(), packageDesc))
						.then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
								.executes(ctx -> spawnPackageFromCommand(game, ctx, EntityArgument.getPlayers(ctx, "target"), Optional.of(DoubleArgumentType.getDouble(ctx, "amount")), packageDesc))
						));
				case RANDOM_PLAYER, ALL_PLAYERS ->
						subcommand.executes(ctx -> spawnPackageFromCommand(game, ctx, null, Optional.empty(), packageDesc))
						.then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0))
								.executes(ctx -> spawnPackageFromCommand(game, ctx, null, Optional.of(DoubleArgumentType.getDouble(ctx, "amount")), packageDesc))
						);
			}
			commands.register(Commands.literal("package").then(subcommand));
		});

		events.listen(GamePackageEvents.RECEIVE_PACKAGE, gamePackage -> onGamePackageReceived(game, gamePackage, packageDesc));

		receiveActions.register(game, events);

		game.state().get(GamePackageState.KEY).addPackageType(packageDesc);
	}

	private int spawnPackageFromCommand(IGamePhase game, CommandContext<CommandSourceStack> ctx, @Nullable Collection<ServerPlayer> targetPlayers, Optional<Double> amount, DonationPackageDesc packageDesc) {
		GamePackage gamePackage = new GamePackage(
				id,
				"LoveTropics",
				targetPlayers != null ? ActionSubjects.ofPlayers(List.copyOf(targetPlayers)) : null,
				amount
		);
		switch (onGamePackageReceived(game, gamePackage, packageDesc)) {
			case TRUE -> ctx.getSource().sendSuccess(() -> Component.translatable("Successfully sent '%s'", id), true);
			case DEFAULT -> ctx.getSource().sendFailure(Component.translatable("'%s' was not processed", id));
			case FALSE -> ctx.getSource().sendFailure(Component.translatable("'%s' was rejected", id));
		}
		return Command.SINGLE_SUCCESS;
	}

	private TriState onGamePackageReceived(IGamePhase game, GamePackage gamePackage, DonationPackageDesc packageDesc) {
		if (!gamePackage.packageType().equals(id)) {
			return TriState.DEFAULT;
		}

		ActionSubjects<?> targets = resolveTargets(game, gamePackage);
		if (targets == null) {
			LOGGER.warn("Expected donation package to have a receiver, but did not resolve any for {}", gamePackage);
			return TriState.FALSE;
		}

		ContextMap context = actionContext(gamePackage, packageDesc);
		if (receiveActions.apply(game, context, targets)) {
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
		return switch (targetMode) {
			case PLAYER, TEAM -> null;
			case RANDOM_PLAYER -> allTargets.takeOneRandom(game, ActionSubjectType.PLAYER);
			case ALL_PLAYERS -> allTargets;
		};
	}

	private ActionSubjects<?> resolveAllTargets(IGamePhase game) {
		// Players in sub-phases are no longer in this phase, but keep their role here
		return ActionSubjects.ofPlayers(
				game.allPlayers(includeSubPhases).filter(player -> game.getRoleFor(player) == PlayerRole.PARTICIPANT)
		);
	}

	private ContextMap actionContext(GamePackage gamePackage, DonationPackageDesc packageDesc) {
		ContextMap.Builder context = new ContextMap.Builder();
		context.withParameter(GameActionContextKeys.PACKAGE, gamePackage);
		context.withParameter(GameActionContextKeys.PACKAGE_DESC, packageDesc);
		if (gamePackage.sendingPlayerName() != null) {
			context.withParameter(GameActionContextKeys.PACKAGE_SENDER, gamePackage.sendingPlayerName());
		}
		return context.create(ContextKeySet.EMPTY);
	}

	public enum TargetSelectionMode {
		PLAYER("player"),
		TEAM("team"),
		RANDOM_PLAYER("random_player"),
		ALL_PLAYERS("all_players"),
		;

		public static final Codec<TargetSelectionMode> CODEC = MoreCodecs.stringVariants(TargetSelectionMode.values(), s -> s.type);

		public final String type;

		TargetSelectionMode(String type) {
			this.type = type;
		}

		public boolean requiresSelection() {
			return this == PLAYER || this == TEAM;
		}

		public DonationPackageDesc.SelectionMode toExternal() {
			return switch (this) {
				case PLAYER -> DonationPackageDesc.SelectionMode.PLAYER;
				case TEAM -> DonationPackageDesc.SelectionMode.TEAM;
				case RANDOM_PLAYER, ALL_PLAYERS -> DonationPackageDesc.SelectionMode.NONE;
			};
		}
	}
}
