package com.lovetropics.minigames.common.content.mangroves_and_pianguas.behavior.plant.placement;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.content.mangroves_and_pianguas.behavior.event.MpPlantEvents;
import com.lovetropics.minigames.common.content.mangroves_and_pianguas.plot.plant.PlantCoverage;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.world.gen.blockplacer.DoublePlantBlockPlacer;
import net.minecraft.world.server.ServerWorld;

public final class PlaceDoublePlantBehavior implements IGameBehavior {
	public static final Codec<PlaceDoublePlantBehavior> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			MoreCodecs.BLOCK_STATE.fieldOf("block").forGetter(c -> c.block)
	).apply(instance, PlaceDoublePlantBehavior::new));

	private final BlockState block;

	public PlaceDoublePlantBehavior(BlockState block) {
		this.block = block;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(MpPlantEvents.PLACE, (player, plot, pos) -> {
			ServerWorld world = game.getWorld();
			DoublePlantBlockPlacer.PLACER.place(world, pos, this.block, world.rand);
			return PlantCoverage.ofDouble(pos);
		});
	}
}
