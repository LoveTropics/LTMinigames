package org.lovetropics.games.common.content.biodiversity_blitz.behavior;

import com.lovetropics.lib.codec.MoreCodecs;
import org.lovetropics.games.common.content.biodiversity_blitz.plot.Plot;
import org.lovetropics.games.common.content.biodiversity_blitz.plot.PlotsState;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// Removes pianguas from mud blocks
public final class RemoveFromBlockBehavior implements IGameBehavior {
	public static final MapCodec<RemoveFromBlockBehavior> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			MoreCodecs.BLOCK_STATE.fieldOf("in").forGetter(b -> b.in),
			MoreCodecs.BLOCK_STATE.fieldOf("out").forGetter(b -> b.out),
			ItemStackTemplate.CODEC.fieldOf("drop").forGetter(b -> b.drop)
	).apply(instance, RemoveFromBlockBehavior::new));

	private final BlockState in;
	private final BlockState out;
	private final ItemStackTemplate drop;

	private PlotsState plots;

	public RemoveFromBlockBehavior(BlockState in, BlockState out, ItemStackTemplate drop) {
		this.in = in;
		this.out = out;
		this.drop = drop;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		plots = game.state().getOrThrow(PlotsState.KEY);

		events.listen(GamePlayerEvents.USE_BLOCK, this::onUseBlock);
	}

	private InteractionResult onUseBlock(ServerPlayer player, ServerLevel level, BlockPos pos, InteractionHand hand, BlockHitResult result) {
		Plot plot = plots.getPlotFor(player);
		if (plot != null && plot.bounds.contains(pos)) {
			if (level.getBlockState(pos).getBlock() == in.getBlock()) {
				level.setBlockAndUpdate(pos, out);

				BlockPos spawnPos = pos.relative(result.getDirection());
				level.addFreshEntity(new ItemEntity(level, spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5, drop.create()));

				return InteractionResult.SUCCESS;
			}
		}

		return InteractionResult.PASS;
	}
}
