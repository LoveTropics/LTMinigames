package org.lovetropics.games.common.core.game.behavior.instances;

import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.rewards.GameRewardsMap;
import org.lovetropics.games.common.util.Util;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public record PlayerHeadRewardBehavior() implements IGameBehavior {
	public static final MapCodec<PlayerHeadRewardBehavior> CODEC = MapCodec.unit(PlayerHeadRewardBehavior::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		GameRewardsMap rewards = game.instanceState().getOrThrow(GameRewardsMap.STATE);
		MutableObject<CompletableFuture<?>> resolvedFuture = new MutableObject<>(CompletableFuture.completedFuture(null));

		events.listen(GamePlayerEvents.DEATH, (target, source) -> {
			ServerPlayer killer = Util.getKillerPlayer(target, source);
			if (killer != null) {
				CompletableFuture<?> future = createPlayerHead(target)
						.thenAcceptAsync(stack -> rewards.forPlayer(killer).giveCollectible(stack), game.server());
				resolvedFuture.setValue(resolvedFuture.get().thenCombine(future, (a, b) -> b));
			}
			return TriState.DEFAULT;
		});

		events.listen(GamePhaseEvents.FINISH, () -> {
			try {
				// Try our best to let these resolve before exiting, but it's not critical
				resolvedFuture.get().get(10, TimeUnit.SECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException ignored) {
			}
		});
	}

	private static CompletableFuture<ItemStack> createPlayerHead(ServerPlayer player) {
		CompletableFuture<ItemStack> future = new CompletableFuture<>();
		Util.getProfile(player.level().getServer(), player.nameAndId().id()).thenAccept(result -> {
			ItemStack head = new ItemStack(Items.PLAYER_HEAD);
			result.ifPresent(profile -> head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile)));
			future.complete(head);
		});
		return future;
	}
}
