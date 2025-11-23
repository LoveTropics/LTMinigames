package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContextKeys;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

public record GiveRespawnAction(

) implements IGameBehavior {
	public static final MapCodec<GiveRespawnAction> CODEC = MapCodec.unit(GiveRespawnAction::new);

	private static final MutableComponent ANONYMOUS_DONOR = Component.literal("A donor");

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		Queue<Component> livesGivenBy = new ArrayDeque<>();

		events.applyToPlayers(game, (context, target) -> {
			String sender = context.getOptional(GameActionContextKeys.PACKAGE_SENDER);
			livesGivenBy.add(sender != null ? Component.literal(sender).withStyle(ChatFormatting.AQUA) : ANONYMOUS_DONOR);
			game.statistics().forPlayer(target).incrementInt(StatisticKey.LIVES, 1);
			return true;
		});

		// TODO: This is a hack, would run more with more instances
		events.listen(GamePlayerEvents.RESPAWN, player -> {
			int livesLeft = game.statistics().forPlayer(player).getInt(StatisticKey.LIVES);
			Component donor = Objects.requireNonNullElse(livesGivenBy.poll(), ANONYMOUS_DONOR);

			PlayerSet players = PlayerSet.of(player);
			players.fadeToBlack(0);

			game.scheduler().runAfterTicks(SharedConstants.TICKS_PER_SECOND, () -> {
				players.fadeFromBlack(SharedConstants.TICKS_PER_SECOND);

				if (game.getRoleFor(player) == PlayerRole.SPECTATOR) {
					players.showTitle(
							Component.literal("You died!").withStyle(ChatFormatting.RED),
							Component.translatable("and have no more lives!", donor),
							0,
							SharedConstants.TICKS_PER_SECOND * 4,
							SharedConstants.TICKS_PER_SECOND
					);
				} else {
					players.showTitle(
							Component.literal("You died!").withStyle(ChatFormatting.RED),
							Component.translatable("..but were respawned by %s", donor),
							0,
							SharedConstants.TICKS_PER_SECOND * 4,
							SharedConstants.TICKS_PER_SECOND
					);
					game.scheduler().runAfterTicks(SharedConstants.TICKS_PER_SECOND * 3, () ->
							players.showTitle(
									Component.translatable("%s lives left",
											Component.literal(String.valueOf(livesLeft)).withStyle(ChatFormatting.RED)
									),
									Component.literal("Donate at ").append(Component.literal("live.lovetropics.org/donate").withStyle(ChatFormatting.AQUA)),
									0,
									SharedConstants.TICKS_PER_SECOND * 3,
									SharedConstants.TICKS_PER_SECOND
							)
					);
				}
			});
		});
	}
}
