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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.List;

public final class IdleDropItemPlantBehavior implements IGameBehavior {
	public static final MapCodec<IdleDropItemPlantBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ItemStackTemplate.CODEC.fieldOf("item").forGetter(b -> b.item),
			Codec.INT.fieldOf("interval").forGetter(b -> b.interval)
	).apply(i, IdleDropItemPlantBehavior::new));
	private final ItemStackTemplate item;
	private final int interval;

	private IGamePhase game;

	public IdleDropItemPlantBehavior(ItemStackTemplate item, int interval) {
		this.item = item;
		this.interval = interval;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		this.game = game;
		events.listen(BbPlantEvents.TICK, this::tickPlants);
	}

	private void tickPlants(PlayerSet players, Plot plot, List<Plant> plants) {
		long ticks = game.ticks();
		RandomSource random = game.level().getRandom();

		if (ticks % interval != 0) {
			return;
		}

		ServerLevel world = game.level();

		for (Plant plant : plants) {
			BlockPos.MutableBlockPos pos = plant.coverage().random(random).mutable();

			for (int i = 0; i < 8; i++) {
				if (world.getBlockState(pos).isAir()) {
					world.addFreshEntity(new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), item.create()));
					break;
				}

				pos.move(Direction.DOWN);
			}
		}
	}
}
