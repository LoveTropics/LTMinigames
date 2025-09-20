package com.lovetropics.minigames.common.core.game.behavior.instances.tweak;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;

public class TntAutoFuseBehavior implements IGameBehavior {
	public static final MapCodec<TntAutoFuseBehavior> CODEC = MapCodec.unit(TntAutoFuseBehavior::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GamePlayerEvents.PLACE_BLOCK, (player, pos, placed, placedOn, placedItemStack) -> {
			if (placed.getBlock() instanceof TntBlock) {
				placed.getBlock().onCaughtFire(placed, player.level(), pos, null, null);
				player.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
			}
			return TriState.DEFAULT;
		});
	}
}
