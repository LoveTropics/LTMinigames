package com.lovetropics.minigames.common.core.game.behavior.instances.world;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameWorldEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.TriState;

import java.util.List;

public record PreventCropGrowthInRegionBehaviour(
		List<String> regions
) implements IGameBehavior {

	public static final MapCodec<PreventCropGrowthInRegionBehaviour> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.listOf().fieldOf("regions").forGetter(c -> c.regions)
	).apply(i, PreventCropGrowthInRegionBehaviour::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		List<BlockBox> allRegions = game.mapRegions().getAll(regions);
		events.listen(GameWorldEvents.CROP_GROW, (level, blockpos) -> {
			if(allRegions.stream().anyMatch(r -> r.contains(blockpos))) {
				return TriState.FALSE;
			}
			return TriState.DEFAULT;
		});
	}
}
