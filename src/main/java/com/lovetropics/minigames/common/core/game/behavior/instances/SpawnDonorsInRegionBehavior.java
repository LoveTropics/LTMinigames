package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.data.LoveTropicsAttachments;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.SpawnDonorUtils;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLivingEntityEvents;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import com.lovetropics.minigames.common.core.integration.BackendIntegrations;
import com.lovetropics.minigames.common.core.integration.GameInstanceIntegrations;
import com.lovetropics.minigames.common.core.integration.game_actions.Donation;
import com.lovetropics.minigames.common.core.integration.state.DonationScale;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;

import java.util.List;
import java.util.Optional;

public record SpawnDonorsInRegionBehavior(
	List<String> regions,
	Optional<List<DonationScale>> scales
) implements IGameBehavior {

	public static final MapCodec<SpawnDonorsInRegionBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.listOf().fieldOf("regions").forGetter(SpawnDonorsInRegionBehavior::regions),
			DonationScale.LIST_CODEC.optionalFieldOf("scales").forGetter(SpawnDonorsInRegionBehavior::scales)
	).apply(i, SpawnDonorsInRegionBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		if (!BackendIntegrations.get().isConnected()) {
			throw new GameException(GameTexts.Status.integrationsNotConnected());
		}
		GameInstanceIntegrations integrations = game.instanceState().getOrNull(GameInstanceIntegrations.KEY);
		if (integrations == null) {
			return;
		}

		integrations.get("donations/donations/all/limited", Donation.LIST_CODEC).thenAcceptAsync(result -> {
				if (result.isPresent()) {
					final List<Donation> donations = result.get();
					for (Donation donation : donations) {
						SpawnDonorUtils.spawnDonorInRandomRegion(game, donation, regions, scales.orElse(List.of()));

					}
				}
			}, game.scheduler()
		);

		events.listen(GameLivingEntityEvents.MOB_DROP, (e, d, r) -> {
			if (e.hasData(LoveTropicsAttachments.DONATION)) {
				ItemStack book = new ItemStack(Items.WRITTEN_BOOK, 1);
				final Donation donation = e.getData(LoveTropicsAttachments.DONATION);
				List<String> donorComments = List.of(donation.comments());

				book.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(donorComments.stream().map(Filterable::passThrough).toList()));
				book.set(DataComponents.CUSTOM_NAME, Component.translatable("ltminigames.minigame.escape_race.donorbook.title", donation.getDisplayName(DyeColor.WHITE.getTextColor(), e.getRandom())));
				r.add(new ItemEntity(e.level(), e.getX(), e.getY(), e.getZ(), book));
			}

			return TriState.DEFAULT;
		});
	}
}

