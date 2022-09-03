package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class SetExtendingBlocksAction implements IGameBehavior {
	public static final Codec<SetExtendingBlocksAction> CODEC = RecordCodecBuilder.create(i -> i.group(
			MoreCodecs.BLOCK_PREDICATE.optionalFieldOf("replace").forGetter(c -> Optional.ofNullable(c.replace)),
			MoreCodecs.BLOCK_STATE_PROVIDER.fieldOf("set").forGetter(c -> c.set),
			MoreCodecs.stringVariants(Direction.values(), Direction::getName).fieldOf("direction").forGetter(c -> c.direction),
			MoreCodecs.arrayOrUnit(Codec.STRING, String[]::new).optionalFieldOf("region", new String[0]).forGetter(c -> c.regionKeys),
			Codec.LONG.fieldOf("duration").forGetter(c -> c.duration),
			Codec.BOOL.optionalFieldOf("notify_neighbors", true).forGetter(c -> c.notifyNeighbors)
	).apply(i, SetExtendingBlocksAction::new));

	private final @Nullable BlockPredicate replace;
	private final BlockStateProvider set;
	private final Direction direction;

	private final String[] regionKeys;
	private final long duration;

	private final boolean notifyNeighbors;

	private long startTime = -1;

	private SetExtendingBlocksAction(Optional<BlockPredicate> replace, BlockStateProvider set, Direction direction, String[] regionKeys, long duration, boolean notifyNeighbors) {
		this(replace.orElse(null), set, direction, regionKeys, duration, notifyNeighbors);
	}

	public SetExtendingBlocksAction(BlockPredicate replace, BlockStateProvider set, Direction direction, String[] regionKeys, long duration, boolean notifyNeighbors) {
		this.replace = replace;
		this.set = set;
		this.direction = direction;
		this.regionKeys = regionKeys;
		this.duration = duration;
		this.notifyNeighbors = notifyNeighbors;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		List<BlockBox> regions = new ArrayList<>();
		for (String regionKey : regionKeys) {
			regions.addAll(game.getMapRegions().get(regionKey));
		}

		if (regions.isEmpty()) {
			throw new GameException(new TextComponent("Regions not specified for extending block set behavior!"));
		}

		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			startTime = game.ticks();
			return true;
		});

		events.listen(GamePhaseEvents.TICK, () -> {
			if (startTime == -1) {
				return;
			}

			long gameTime = game.ticks();
			long endTime = startTime + duration;
			if (gameTime >= startTime && gameTime < endTime) {
				long time = gameTime - startTime;
				long timeLength = endTime - startTime + 1;

				float progress = Mth.clamp((float) time / timeLength, 0.0F, 1.0F);

				for (BlockBox region : regions) {
					this.tickExtendingInBox(game, region, progress);
				}
			}
		});
	}

	private void tickExtendingInBox(IGamePhase game, BlockBox box, float progress) {
		// TODO: we should be not rewriting blocks that we already placed
		BlockBox extendingBox = this.getExtendingBox(box, progress);

		ServerLevel world = game.getWorld();
		BlockPredicate replace = this.replace;
		BlockStateProvider set = this.set;
		Random random = world.random;

		int flags = Block.UPDATE_ALL;
		if (!notifyNeighbors) {
			// the constant name is inverted
			flags |= Block.UPDATE_KNOWN_SHAPE;
		}

		for (BlockPos pos : extendingBox) {
			if (replace == null || replace.matches(world, pos)) {
				BlockState state = set.getState(random, pos);
				state = Block.updateFromNeighbourShapes(state, world, pos);
				world.setBlock(pos, state, flags);
			}
		}
	}

	private BlockBox getExtendingBox(BlockBox box, float progress) {
		BlockPos size = box.size();

		int totalLength = direction.getAxis().choose(size.getX(), size.getY(), size.getZ());
		int currentLength = Mth.floor(totalLength * progress);

		if (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
			return box.withMax(box.min().relative(direction, currentLength));
		} else {
			return box.withMin(box.max().relative(direction, currentLength));
		}
	}
}
