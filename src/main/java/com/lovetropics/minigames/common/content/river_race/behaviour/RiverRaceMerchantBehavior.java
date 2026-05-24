package com.lovetropics.minigames.common.content.river_race.behaviour;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.content.biodiversity_blitz.BiodiversityBlitzTexts;
import com.lovetropics.minigames.common.content.biodiversity_blitz.merchant.BbMerchant;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// TODO Merge with BbMerchantBehavior?
public class RiverRaceMerchantBehavior implements IGameBehavior {
	private static final Codec<MerchantOffer> OFFER_CODEC = RecordCodecBuilder.create(i -> i.group(
			ItemCost.CODEC.fieldOf("input").forGetter(MerchantOffer::getItemCostA),
			ItemStackTemplate.CODEC.fieldOf("output").forGetter(merchantOffer -> ItemStackTemplate.fromNonEmptyStack(merchantOffer.getResult()))
	).apply(i, (input, output) -> new MerchantOffer(input, output.create(), Integer.MAX_VALUE, 0, 0)));

	public static final MapCodec<RiverRaceMerchantBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("zone").forGetter(c -> c.region),
			BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity").forGetter(c -> c.entity),
			ComponentSerialization.CODEC.optionalFieldOf("name", CommonComponents.EMPTY).forGetter(c -> c.name),
			OFFER_CODEC.listOf().fieldOf("offers").forGetter(c -> c.offers)
	).apply(i, RiverRaceMerchantBehavior::new));

	private final String region;
	private final EntityType<?> entity;
	private final Component name;
	private final List<MerchantOffer> offers;

	private final Set<UUID> merchants = new ObjectOpenHashSet<>();

	private IGamePhase game;

	public RiverRaceMerchantBehavior(String region, EntityType<?> entity, Component name, List<MerchantOffer> offers) {
		this.region = region;
		this.entity = entity;
		this.name = name;
		this.offers = offers;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		this.game = game;

		events.listen(GamePhaseEvents.CREATE, this::onGameStarted);
		events.listen(GamePlayerEvents.INTERACT_ENTITY, this::interactWithEntity);
	}

	/**
	 * When the game loads, load this merchant into its proper section
	 */
	private void onGameStarted() {
		ServerLevel level = game.level();
		List<BlockBox> regions = game.mapRegions().getAll(region);
		for (BlockBox region : regions) {
			Vec3 center = region.center();

			Entity merchant = createMerchant(level);
			if (merchant == null) {
				return;
			}
			merchant.snapTo(center.x(), center.y() - 0.5, center.z(), 0, 0);

			level.getChunk(region.centerBlock());
			level.addFreshEntity(merchant);

			if (merchant instanceof Mob mob) {
				mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(center)), EntitySpawnReason.MOB_SUMMONED, null);
				mob.setNoAi(true);
				mob.setBaby(false);
				mob.setInvulnerable(true);
				mob.setPersistenceRequired();
			}

			merchants.add(merchant.getUUID());
		}
	}

	private InteractionResult interactWithEntity(ServerPlayer player, Entity target, InteractionHand hand) {
		if (merchants.contains(target.getUUID())) {
			MerchantOffers builtOffers = new MerchantOffers();
			for (MerchantOffer offer : offers) {
				builtOffers.add(offer.copy());
			}

			// TODO need a different screen?
			BbMerchant merchant = new BbMerchant(player, builtOffers);
			merchant.openTradingScreen(player, BiodiversityBlitzTexts.TRADING, 1);

			return InteractionResult.SUCCESS;
		}

		return InteractionResult.PASS;
	}

	@Nullable
	private Entity createMerchant(ServerLevel world) {
		Entity merchant = entity.create(world, EntitySpawnReason.COMMAND);
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
}
