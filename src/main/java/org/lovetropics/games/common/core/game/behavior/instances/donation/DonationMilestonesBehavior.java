package org.lovetropics.games.common.core.game.behavior.instances.donation;

import com.google.common.base.Strings;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.GameActionContextKeys;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePackageEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.instances.command.CustomCommandBehavior;
import org.lovetropics.games.common.core.game.behavior.instances.donation.DonationMilestones.Progress;
import org.lovetropics.games.common.core.game.state.GameStateMap;
import org.lovetropics.games.common.core.game.util.GameTexts;

public final class DonationMilestonesBehavior implements IGameBehavior {
	private static final Codec<String> ID_CODEC = Codec.STRING.validate(id -> id.matches("[a-z0-9_]+")
			? DataResult.success(id)
			: DataResult.error(() -> "Donation milestones id must only use a-z, 0-9 and _, to be typed in commands: " + id));

	public static final MapCodec<DonationMilestonesBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ID_CODEC.fieldOf("id").forGetter(b -> b.id),
			Codec.doubleRange(Progress.MIN_AMOUNT, Double.MAX_VALUE).fieldOf("every").forGetter(b -> b.every),
			GameActionList.MAP_CODEC.forGetter(b -> b.actions)
	).apply(i, DonationMilestonesBehavior::new));

	private final String id;
	private final double every;
	private final GameActionList actions;

	private DonationMilestones milestones;

	public DonationMilestonesBehavior(String id, double every, GameActionList actions) {
		this.id = id;
		this.every = every;
		this.actions = actions;
	}

	@Override
	public void registerState(IGamePhase game, GameStateMap phaseState, GameStateMap instanceState) {
		milestones = phaseState.getOrRegister(DonationMilestones.KEY, new DonationMilestones());
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		actions.register(game, events);

		Progress progress = milestones.add(id, every);
		events.listen(GamePackageEvents.RECEIVE_DONATION, donation -> {
			String sender = !Strings.isNullOrEmpty(donation.name()) && !donation.anonymous() ? donation.name() : null;
			donate(game, progress, donation.amount(), sender);
		});

		// Every milestone registers the same listing of all of them, and its own subcommand
		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, _) -> commands.register(Commands.literal("donation_milestones")
				.requires(source -> CustomCommandBehavior.PermissionLevel.OVERLORD.hasPermission(game, source))
				.executes(this::list)
				.then(Commands.literal(id)
						.executes(context -> sendProgress(context, progress, false))
						.then(Commands.literal("every")
								.then(Commands.argument("amount", DoubleArgumentType.doubleArg(Progress.MIN_AMOUNT))
										.executes(context -> {
											progress.setEvery(DoubleArgumentType.getDouble(context, "amount"));
											return sendProgress(context, progress, true);
										})
								)
						)
						.then(Commands.literal("donate")
								.then(Commands.argument("amount", DoubleArgumentType.doubleArg(Progress.MIN_AMOUNT))
										.executes(context -> {
											int reached = donate(game, progress, DoubleArgumentType.getDouble(context, "amount"), null);
											context.getSource().sendSuccess(() -> GameTexts.Commands.DONATION_MILESTONES_REACHED.apply(progress.name(), reached), true);
											return sendProgress(context, progress, false);
										})
								)
						)
				)
		));
	}

	/// @return how many milestones the donation reached
	private int donate(IGamePhase game, Progress progress, double amount, @Nullable String sender) {
		int reached = progress.donate(amount);
		if (reached > 0) {
			ContextMap context = actionContext(sender);
			for (int i = 0; i < reached; i++) {
				actions.apply(game, context);
			}
		}
		return reached;
	}

	private static ContextMap actionContext(@Nullable String sender) {
		ContextMap.Builder context = new ContextMap.Builder();
		if (sender != null) {
			context.withParameter(GameActionContextKeys.SENDER, sender);
		}
		return context.create(ContextKeySet.EMPTY);
	}

	private int list(CommandContext<CommandSourceStack> context) {
		for (Progress progress : milestones.all()) {
			context.getSource().sendSuccess(progress::describe, false);
		}
		return milestones.all().size();
	}

	private static int sendProgress(CommandContext<CommandSourceStack> context, Progress progress, boolean broadcast) {
		context.getSource().sendSuccess(progress::describe, broadcast);
		return 1;
	}
}
