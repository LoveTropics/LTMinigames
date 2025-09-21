package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant;

import com.lovetropics.minigames.common.content.biodiversity_blitz.BiodiversityBlitzTexts;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbPlantEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.Plant;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.PlantPlacement;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventListeners;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public record PlantBiomeCheckBehavior(HolderSet<Biome> biomes, boolean whitelist, IGameBehavior behaviors) implements IGameBehavior {
	public static final MapCodec<PlantBiomeCheckBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Biome.LIST_CODEC.fieldOf("biomes").forGetter(PlantBiomeCheckBehavior::biomes),
			Codec.BOOL.optionalFieldOf("whitelist", true).forGetter(PlantBiomeCheckBehavior::whitelist),
			IGameBehavior.CODEC.optionalFieldOf("behaviors", IGameBehavior.EMPTY).forGetter(PlantBiomeCheckBehavior::behaviors)
	).apply(i, PlantBiomeCheckBehavior::new));

	private static final Set<GameEventType<?>> WRAPPED_EVENTS = Set.of(BbPlantEvents.TICK, BbPlantEvents.PLACE, BbPlantEvents.BREAK);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		final GameEventListeners checkedListeners = new GameEventListeners();

		behaviors.register(game, events.redirect(WRAPPED_EVENTS::contains, checkedListeners));

		events.listen(BbPlantEvents.TICK, (players, plot, plants) -> {
			final List<Plant> actualPlants = new ArrayList<>(plants);
			actualPlants.removeIf(plant -> !canContinue(game, plant));
			checkedListeners.invoker(BbPlantEvents.TICK).onTickPlants(players, plot, plants);
		});

		events.listen(BbPlantEvents.PLACE, (player, plot, pos) -> {
			final var placement = checkedListeners.invoker(BbPlantEvents.PLACE).placePlant(player, plot, pos);
			if (placement != null && !canContinue(game, pos)) {
				player.displayClientMessage(BiodiversityBlitzTexts.PLANT_CANNOT_BE_PLACED_IN_BIOME.copy().withStyle(ChatFormatting.RED), true);
				player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
				return new PlantPlacement();
			}
			return placement;
		});

		events.listen(BbPlantEvents.BREAK, (player, plot, plant, pos) -> {
			if (canContinue(game, plant)) {
				checkedListeners.invoker(BbPlantEvents.BREAK).breakPlant(player, plot, plant, pos);
			}
		});
	}

	private boolean canContinue(IGamePhase game, Plant plant) {
		return canContinue(game, plant.coverage().getOrigin());
	}

	private boolean canContinue(IGamePhase game, BlockPos pos) {
		// If blacklist and doesn't contain -> allow. If whitelist and contains -> allow
		return biomes.contains(game.level().getBiome(pos)) == whitelist;
	}
}
