package com.lovetropics.minigames.common.content.biodiversity_blitz.behavior;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.content.biodiversity_blitz.BiodiversityBlitzTexts;
import com.lovetropics.minigames.common.content.biodiversity_blitz.behavior.event.BbEvents;
import com.lovetropics.minigames.common.content.biodiversity_blitz.merchant.BbMerchant;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.Plot;
import com.lovetropics.minigames.common.content.biodiversity_blitz.plot.plant.PlantItemType;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.util.EntityTemplate;
import com.lovetropics.minigames.common.util.Util;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public final class BbMerchantBehavior implements IGameBehavior {
	public static final MapCodec<BbMerchantBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("plot_region").forGetter(c -> c.plotRegion),
			EntityTemplate.CODEC.fieldOf("entity").forGetter(c -> c.entity),
			ComponentSerialization.CODEC.optionalFieldOf("name", CommonComponents.EMPTY).forGetter(c -> c.name),
			Offer.CODEC.listOf().fieldOf("offers").forGetter(c -> c.offers)
	).apply(i, BbMerchantBehavior::new));

	private final String plotRegion;
	private final EntityTemplate entity;
	private final Component name;
	private final List<Offer> offers;

	private final Set<UUID> merchants = new ObjectOpenHashSet<>();

	private IGamePhase game;

	public BbMerchantBehavior(String plotRegion, EntityTemplate entity, Component name, List<Offer> offers) {
		this.plotRegion = plotRegion;
		this.entity = entity;
		this.name = name;
		this.offers = offers;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		this.game = game;

		events.listen(BbEvents.CREATE_PLOT, this::onCreatePlot);
		events.listen(GamePlayerEvents.INTERACT_ENTITY, this::interactWithEntity);
	}

	private void onCreatePlot(Plot plot) {
		ServerLevel world = game.level();

		BlockBox region = plot.regionByName(plotRegion);
		if (region == null) {
			return;
		}

		Vec3 center = region.center();
		Direction direction = Util.getDirectionBetween(region, plot.spawn);
		float yaw = direction.toYRot();

		Entity merchant = createMerchant(world, center.x(), center.y() - 0.5, center.z(), yaw, 0.0f);
		if (merchant == null) {
			return;
		}

		merchant.setYHeadRot(yaw);

		world.getChunk(region.centerBlock());
		world.addFreshEntity(merchant);

		if (merchant instanceof Mob mob) {
			mob.finalizeSpawn(world, world.getCurrentDifficultyAt(BlockPos.containing(center)), EntitySpawnReason.MOB_SUMMONED, null);
			mob.setNoAi(true);
			mob.setBaby(false);
			mob.setInvulnerable(true);
			mob.setPersistenceRequired();
		}

		merchants.add(merchant.getUUID());
	}

	@Nullable
	private Entity createMerchant(ServerLevel level, double x, double y, double z, float yRot, float xRot) {
		Entity merchant = entity.create(level, x, y, z, yRot, xRot);
		if (merchant != null) {
			if (name != CommonComponents.EMPTY) {
				merchant.setCustomName(name);
				merchant.setCustomNameVisible(true);
			}

			merchant.setSilent(true);

			return merchant;
		}

		return null;
	}

	private InteractionResult interactWithEntity(ServerPlayer player, Entity target, InteractionHand hand) {
		if (merchants.contains(target.getUUID())) {
			MerchantOffers builtOffers = new MerchantOffers();
			for (Offer offer : offers) {
				builtOffers.add(offer.build(game));
			}

			BbMerchant merchant = new BbMerchant(player, builtOffers);
			merchant.openTradingScreen(player, BiodiversityBlitzTexts.TRADING, 1);

			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	public static final class Offer {
		public static final Codec<Offer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ItemCost.CODEC.fieldOf("input").forGetter(c -> c.input),
				Output.CODEC.fieldOf("output").forGetter(c -> c.output)
		).apply(instance, Offer::new));

		private final ItemCost input;
		private final Output output;

		public Offer(ItemCost input, Output output) {
			this.input = input;
			this.output = output;
		}

		public MerchantOffer build(IGamePhase game) {
			return new MerchantOffer(
					input, output.build(game),
					Integer.MAX_VALUE,
					0,
					0
			);
		}
	}

	public static final class Output {
		private static final Codec<Output> ITEM_CODEC = ItemStackTemplate.CODEC.xmap(Output::item, output -> output.item);
		private static final Codec<Output> PLANT_CODEC = RecordCodecBuilder.create(i -> i.group(
				PlantItemType.CODEC.fieldOf("plant").forGetter(c -> c.plant)
		).apply(i, Output::plant));

		public static final Codec<Output> CODEC = Codec.either(ITEM_CODEC, PLANT_CODEC)
				.xmap(
						either -> either.map(Function.identity(), Function.identity()),
						output -> output.item != null ? Either.left(output) : Either.right(output)
				);

		@Nullable
		private final ItemStackTemplate item;
		@Nullable
		private final PlantItemType plant;

		private Output(@Nullable ItemStackTemplate item, @Nullable PlantItemType plant) {
			this.item = item;
			this.plant = plant;
		}

		private static Output item(ItemStackTemplate item) {
			return new Output(item, null);
		}

		private static Output plant(PlantItemType plant) {
			return new Output(null, plant);
		}

		private ItemStack build(IGamePhase game) {
			if (item != null) {
				return item.create();
			} else if (plant != null) {
				return game.invoker(BbEvents.CREATE_PLANT_ITEM).createPlantItem(plant);
			}
			throw new IllegalStateException();
		}
	}
}
