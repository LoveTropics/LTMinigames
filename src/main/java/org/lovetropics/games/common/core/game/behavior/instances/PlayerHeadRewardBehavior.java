package org.lovetropics.games.common.core.game.behavior.instances;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.rewards.GameRewardsMap;
import org.lovetropics.games.common.util.Util;

public record PlayerHeadRewardBehavior() implements IGameBehavior {
	public static final MapCodec<PlayerHeadRewardBehavior> CODEC = MapCodec.unit(PlayerHeadRewardBehavior::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		GameRewardsMap rewards = game.instanceState().getOrThrow(GameRewardsMap.STATE);

		events.listen(GamePlayerEvents.DEATH, (target, source) -> {
			ServerPlayer killer = Util.getKillerPlayer(target, source);
			if (killer != null) {
				ItemStack head = new ItemStack(Items.PLAYER_HEAD);
				head.set(DataComponents.PROFILE, target.getProfile());
				rewards.forPlayer(killer).giveCollectible(head);
			}
			return TriState.DEFAULT;
		});
	}
}
