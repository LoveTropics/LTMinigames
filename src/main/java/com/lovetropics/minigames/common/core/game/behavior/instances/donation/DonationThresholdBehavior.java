package com.lovetropics.minigames.common.core.game.behavior.instances.donation;

import com.google.common.base.Strings;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContextKeys;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePackageEvents;
import com.lovetropics.minigames.common.core.integration.game_actions.Donation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.util.context.ContextMap;

public record DonationThresholdBehavior(double threshold, GameActionList<ServerPlayer> actions) implements IGameBehavior {
	public static final MapCodec<DonationThresholdBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.DOUBLE.fieldOf("threshold").forGetter(DonationThresholdBehavior::threshold),
			GameActionList.PLAYER_CODEC.fieldOf("actions").forGetter(DonationThresholdBehavior::actions)
	).apply(i, DonationThresholdBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		actions.register(game, events);

		events.listen(GamePackageEvents.RECEIVE_DONATION, donation -> {
			if (donation.amount() >= threshold) {
				ContextMap context = actionContext(donation);
				actions.apply(game, context);
			}
		});
	}

	private static ContextMap actionContext(Donation donation) {
		ContextMap.Builder context = new ContextMap.Builder();
		if (!Strings.isNullOrEmpty(donation.name()) && !donation.anonymous()) {
			context.withParameter(GameActionContextKeys.PACKAGE_SENDER, donation.name());
		}
		return context.create(ContextKeySet.EMPTY);
	}
}
