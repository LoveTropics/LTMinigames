package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.lovetropics.lib.item.StackedHat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;

import java.util.ArrayList;
import java.util.List;

public record GiveHatsAction(
		WeightedList<StackedHat> hats,
		boolean bottom,
		IntProvider count
) implements IGameBehavior {
	public static final MapCodec<GiveHatsAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			WeightedList.nonEmptyCodec(StackedHat.CODEC).fieldOf("hats").forGetter(GiveHatsAction::hats),
			Codec.BOOL.optionalFieldOf("bottom", true).forGetter(GiveHatsAction::bottom),
			IntProviders.POSITIVE_CODEC.optionalFieldOf("count", new ConstantInt(1)).forGetter(GiveHatsAction::count)
	).apply(i, GiveHatsAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.applyToEntities(game, (context, level, entity) -> {
			RandomSource random = game.random();
			List<StackedHat> newHats = new ArrayList<>(StackedHat.getEntityHats(entity));
			for (int i = 0; i < count.sample(random); i++) {
				StackedHat hat = hats.getRandomOrThrow(random);
				if (bottom) {
					newHats.addFirst(hat);
				} else {
					newHats.addLast(hat);
				}
			}
			StackedHat.setEntityHats(entity, newHats);
			return true;
		});
	}

	@Override
	public MapCodec<GiveHatsAction> codec() {
		return CODEC;
	}
}
