package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.SpawnDonorUtils;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import com.lovetropics.minigames.common.core.integration.BackendIntegrations;
import com.lovetropics.minigames.common.core.integration.GameInstanceIntegrations;
import com.lovetropics.minigames.common.core.integration.state.MinecrafterDonor;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record SpawnDonorsInRegionBehavior(
	List<String> regions
) implements IGameBehavior {

	public static final MapCodec<SpawnDonorsInRegionBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.listOf().fieldOf("regions").forGetter(c -> c.regions)
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

		integrations.get("donations/donations/all/limited", MinecrafterDonor.LIST_CODEC).thenAcceptAsync(result -> {
				if (result.isPresent()) {
					final List<MinecrafterDonor> donors = result.get();
					for (MinecrafterDonor donor : donors) {
						SpawnDonorUtils.spawnDonorInRandomRegion(game, donor, regions);
						System.out.println("Spawning " + donor.minecraftName());
					}
				}
			}, game.scheduler()
		);
	}
}

