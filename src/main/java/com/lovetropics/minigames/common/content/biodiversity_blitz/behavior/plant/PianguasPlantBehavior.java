package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbPlantEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.Plant;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class PianguasPlantBehavior implements IGameBehavior {
	public static final MapCodec<PianguasPlantBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.fieldOf("radius").forGetter(b -> b.radius),
			MoreCodecs.BLOCK_STATE.fieldOf("block").forGetter(c -> c.state)
	).apply(i, PianguasPlantBehavior::new));
	private static final TagKey<Block> MUD = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("tropicraft", "mud"));
	private static final int INTERVAL_TICKS = SharedConstants.TICKS_PER_SECOND * 15;

	private final int radius;
	private final BlockState state;

	private IGamePhase game;

	public PianguasPlantBehavior(int radius, BlockState state) {
		this.radius = radius;
		this.state = state;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		this.game = game;
		events.listen(BbPlantEvents.TICK, this::tickPlants);
	}

	private void tickPlants(PlayerSet players, Plot plot, List<Plant> plants) {
		long ticks = game.ticks();
		RandomSource random = game.level().getRandom();

		// TODO: rebalance
		if (ticks % INTERVAL_TICKS != 0 || random.nextInt(4) != 0) {
			return;
		}

		ServerLevel world = game.level();

		for (Plant plant : plants) {
			int dx = random.nextInt(radius) - random.nextInt(radius);
			int dz = random.nextInt(radius) - random.nextInt(radius);

			BlockPos check = plant.coverage().getOrigin().offset(dx, -1, dz);

			if (world.getBlockState(check).is(MUD)) {
				world.setBlockAndUpdate(check, state);
			}
		}
	}
}
