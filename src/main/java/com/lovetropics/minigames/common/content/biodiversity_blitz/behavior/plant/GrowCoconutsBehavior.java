package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant;

import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbPlantEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.Plant;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GrowCoconutsBehavior implements IGameBehavior {

	public static final MapCodec<GrowCoconutsBehavior> CODEC = Codec.INT.fieldOf("interval")
			.xmap(GrowCoconutsBehavior::new, b -> b.interval);

	private static final DeferredHolder<Block, Block> COCONUT = DeferredHolder.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("tropicraft", "coconut"));
	private final int interval;
	private final WeakHashMap<Plant, List<Pair<BlockPos, Direction>>> candidatePositions = new WeakHashMap<>();
	
	private IGamePhase game;

	public GrowCoconutsBehavior(int interval) {
		this.interval = interval;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		this.game = game;
		events.listen(BbPlantEvents.TICK, this::tick);
		events.listen(GamePlayerEvents.BREAK_BLOCK, this::breakFromCoconut);
		events.listen(BbPlantEvents.BREAK, this::onPlantBreak);
	}

	private void tick(PlayerSet players, Plot plot, List<Plant> plants) {
		ServerLevel level = game.level();
		if (game.ticks() % interval == 0) {
			for (Plant plant : plants) {
				List<Pair<BlockPos, Direction>> candidates = candidatePositions.computeIfAbsent(plant, p -> p.functionalCoverage().stream()
					.filter(bp -> level.getBlockState(bp).is(BlockTags.LOGS))
					.filter(bp -> IntStream.range(0, 4)
							.mapToObj(Direction::from2DDataValue)
							.allMatch(d -> level.getBlockState(bp.relative(d).above()).is(BlockTags.LEAVES)))
					.flatMap(bp -> {
						List<Pair<BlockPos, Direction>> ret = new ArrayList<>();
						for (int i = 0; i < 4; i++) {
							Direction dir = Direction.from2DDataValue(i);
							BlockPos pos = bp.relative(dir);
							if (level.isEmptyBlock(pos) || level.getBlockState(pos).getBlock() == COCONUT.get()) {
								ret.add(Pair.of(pos, dir));
							}
						}
						return ret.stream();
					}).collect(Collectors.toList()));

				Collections.shuffle(candidates);
				for (Pair<BlockPos, Direction> candidate : candidates) {
					if (level.isEmptyBlock(candidate.getLeft())) {
						level.setBlockAndUpdate(candidate.getLeft(), COCONUT.get().defaultBlockState().setValue(DirectionalBlock.FACING, candidate.getRight().getOpposite()));
						break;
					}
				}
			}
		}
	}

	// TODO: manual handling of coverage is not a good solution! next year: change our approach to how we're handling dynamic coverages like this
	private TriState breakFromCoconut(ServerPlayer player, BlockPos pos, BlockState state, InteractionHand hand) {
		if (state.is(COCONUT)) {
			BlockPos trunkPos = pos.relative(state.getValue(DirectionalBlock.FACING));
			game.invoker(GamePlayerEvents.BREAK_BLOCK).onBreakBlock(player, trunkPos, player.level().getBlockState(trunkPos), InteractionHand.MAIN_HAND);
			return TriState.FALSE;
		}
		return TriState.TRUE;
	}

	private void onPlantBreak(ServerPlayer player, Plot plot, Plant plant, BlockPos pos) {
		List<Pair<BlockPos, Direction>> candidates = candidatePositions.get(plant);
		if (candidates != null) {
			candidates.forEach(p -> player.level().destroyBlock(p.getLeft(), false));
		}
	}
}
