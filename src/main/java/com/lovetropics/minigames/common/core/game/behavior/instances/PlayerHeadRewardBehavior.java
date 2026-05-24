package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.rewards.GameRewardsMap;
import com.lovetropics.minigames.common.util.Util;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public record PlayerHeadRewardBehavior() implements IGameBehavior {
	public static final MapCodec<PlayerHeadRewardBehavior> CODEC = MapCodec.unit(PlayerHeadRewardBehavior::new);

	@Override
	public void register(final IGamePhase game, final EventRegistrar events) throws GameException {
		final GameRewardsMap rewards = game.instanceState().getOrThrow(GameRewardsMap.STATE);
		final MutableObject<CompletableFuture<?>> resolvedFuture = new MutableObject<>(CompletableFuture.completedFuture(null));

		events.listen(GamePlayerEvents.DEATH, (target, source) -> {
			final ServerPlayer killer = Util.getKillerPlayer(target, source);
			if (killer != null) {
				final CompletableFuture<?> future = createPlayerHead(target)
						.thenAcceptAsync(stack -> rewards.forPlayer(killer).giveCollectible(stack), game.server());
				resolvedFuture.setValue(resolvedFuture.getValue().thenCombine(future, (a, b) -> b));
			}
			return TriState.DEFAULT;
		});

		events.listen(GamePhaseEvents.FINISH, () -> {
			try {
				// Try our best to let these resolve before exiting, but it's not critical
				resolvedFuture.getValue().get(10, TimeUnit.SECONDS);
			} catch (final InterruptedException | ExecutionException | TimeoutException ignored) {
			}
		});
	}

	private static CompletableFuture<ItemStack> createPlayerHead(final ServerPlayer player) {
		final CompletableFuture<ItemStack> future = new CompletableFuture<>();
		Util.getProfile(player.level().getServer(), player.nameAndId().id()).thenAccept(result -> {
			final ItemStack head = new ItemStack(Items.PLAYER_HEAD);
			result.ifPresent(profile -> head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile)));
			future.complete(head);
		});
		return future;
	}
}
