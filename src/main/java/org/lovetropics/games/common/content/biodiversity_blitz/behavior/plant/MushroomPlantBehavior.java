package org.lovetropics.games.common.content.biodiversity_blitz.behavior.plant;

import com.lovetropics.lib.BlockBox;
import org.lovetropics.games.common.content.biodiversity_blitz.BiodiversityBlitz;
import org.lovetropics.games.common.content.biodiversity_blitz.plot.Plot;
import org.lovetropics.games.common.content.biodiversity_blitz.plot.PlotsState;
import org.lovetropics.games.common.content.biodiversity_blitz.plot.plant.Plant;
import org.lovetropics.games.common.content.biodiversity_blitz.plot.plant.PlantType;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameLivingEntityEvents;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public class MushroomPlantBehavior implements IGameBehavior {
	public static final MapCodec<MushroomPlantBehavior> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			PlantType.CODEC.fieldOf("id").forGetter(c -> c.plantType)
	).apply(instance, MushroomPlantBehavior::new));
	private final PlantType plantType;

	public MushroomPlantBehavior(PlantType plantType) {
		this.plantType = plantType;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameLivingEntityEvents.MOB_DROP, (level, e, d, r) -> {
			RandomSource random = e.getRandom();
			Plot plot = game.state().getOrThrow(PlotsState.KEY).getPlotAt(e.blockPosition());
			BlockPos p = e.blockPosition();
			BlockBox b = new BlockBox(p.offset(-2, -2, -2), p.offset(2, 2, 2));

			for (BlockPos pos : b) {
				if (random.nextBoolean()) {
					continue;
				}

				Plant plant = plot.plants.getPlantAt(pos);

				// Mushrooms will cause nearby dying entities to drop extra loot
				if (plant != null && plantType.equals(plant.type())) {
					// TODO: extra bonus in shade
					r.add(new ItemEntity(e.level(), e.getX(), e.getY(), e.getZ(), new ItemStack(BiodiversityBlitz.OSA_POINT.get(), 1)));
					break;
				}
			}

			return TriState.DEFAULT;
		});
	}
}
