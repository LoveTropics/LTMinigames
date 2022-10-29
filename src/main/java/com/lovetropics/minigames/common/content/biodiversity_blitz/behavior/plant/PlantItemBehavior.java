package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.plant;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.content.biodiversity_blitz.BiodiversityBlitzTexts;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.tutorial.BbTutorialState;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.PlotsState;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.Plant;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.PlantItemType;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.PlantType;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionResult;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;

public final class PlantItemBehavior implements IGameBehavior {
	public static final Codec<PlantItemBehavior> CODEC = RecordCodecBuilder.create(i -> i.group(
			PlantItemType.CODEC.fieldOf("id").forGetter(c -> c.itemType),
			PlantType.CODEC.fieldOf("places").forGetter(c -> c.places),
			MoreCodecs.ITEM_STACK.fieldOf("item").forGetter(c -> c.item)
	).apply(i, PlantItemBehavior::new));

	private final PlantItemType itemType;
	private final PlantType places;
	private final ItemStack item;

	private IGamePhase game;
	private PlotsState plots;
	private BbTutorialState tutorial;

	public PlantItemBehavior(PlantItemType itemType, PlantType places, ItemStack item) {
		this.itemType = itemType;
		this.places = places;
		this.item = item;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		this.game = game;
		this.plots = game.getState().getOrThrow(PlotsState.KEY);
		this.tutorial = game.getState().getOrThrow(BbTutorialState.KEY);

		events.listen(GamePlayerEvents.PLACE_BLOCK, this::onPlaceBlock);
		events.listen(BbEvents.CREATE_PLANT_ITEM, this::createPlantDrop);
	}

	private InteractionResult onPlaceBlock(ServerPlayer player, BlockPos pos, BlockState placed, BlockState placedOn) {
		if (!tutorial.isTutorialFinished()) {
			return InteractionResult.PASS;
		}

		ItemStack heldItem = player.getMainHandItem();
		if (!this.itemType.matches(heldItem)) {
			return InteractionResult.PASS;
		}

		Plot plot = plots.getPlotFor(player);
		if (plot != null && plot.plantBounds.contains(pos)) {
			if (plot.plants.getPlantAt(pos) != null) {
				return InteractionResult.FAIL;
			}

			InteractionResultHolder<Plant> result = game.invoker(BbEvents.PLACE_PLANT).placePlant(player, plot, pos, this.places);
			if (result.getObject() == null) {
				player.displayClientMessage(BiodiversityBlitzTexts.plantCannotFit().withStyle(ChatFormatting.RED), true);
				player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
			}

			return result.getResult();
		}

		return InteractionResult.PASS;
	}

	private ItemStack createPlantDrop(PlantItemType itemType) {
		if (this.itemType.equals(itemType)) {
			ItemStack dropItem = this.item.copy();
			this.itemType.applyTo(dropItem);

			return dropItem;
		}

		return ItemStack.EMPTY;
	}
}
