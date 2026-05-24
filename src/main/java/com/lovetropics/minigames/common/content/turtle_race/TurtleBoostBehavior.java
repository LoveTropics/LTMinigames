package com.lovetropics.minigames.common.content.turtle_race;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public record TurtleBoostBehavior(float amount, int duration) implements IGameBehavior {
	public static final MapCodec<TurtleBoostBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.FLOAT.fieldOf("amount").forGetter(TurtleBoostBehavior::amount),
			Codec.INT.fieldOf("duration").forGetter(TurtleBoostBehavior::duration)
	).apply(i, TurtleBoostBehavior::new));

	private static final long NOT_BOOSTING = -1;

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		Multimap<Holder<Attribute>, AttributeModifier> modifiers = ImmutableMultimap.<Holder<Attribute>, AttributeModifier>builder()
				.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(LoveTropics.id("turtle_boost"), amount, AttributeModifier.Operation.ADD_VALUE))
				.build();

		Object2LongMap<UUID> boostEndTimes = new Object2LongArrayMap<>();
		boostEndTimes.defaultReturnValue(NOT_BOOSTING);

		events.applyToPlayers(game, (context, player) -> {
			long endTime = game.ticks() + duration;
			if (boostEndTimes.put(player.getUUID(), endTime) == NOT_BOOSTING) {
				startBoosting(player, modifiers);
			}
			return true;
		});

		events.listen(GamePhaseEvents.TICK, () -> {
			if (boostEndTimes.isEmpty()) {
				return;
			}

			boostEndTimes.object2LongEntrySet().removeIf(entry -> {
				if (game.ticks() < entry.getLongValue()) {
					return false;
				}
				ServerPlayer player = game.allPlayers().getPlayerBy(entry.getKey());
				if (player != null) {
					stopBoosting(player, modifiers);
				}
				return true;
			});
		});

		events.listen(GamePlayerEvents.REMOVE, player -> {
			if (boostEndTimes.removeLong(player.getUUID()) != NOT_BOOSTING) {
				stopBoosting(player, modifiers);
			}
		});
	}

	private static void startBoosting(ServerPlayer player, Multimap<Holder<Attribute>, AttributeModifier> modifiers) {
		if (player.getRootVehicle() instanceof LivingEntity vehicle) {
			vehicle.getAttributes().addTransientAttributeModifiers(modifiers);
		}
	}

	private static void stopBoosting(ServerPlayer player, Multimap<Holder<Attribute>, AttributeModifier> modifiers) {
		if (player.getRootVehicle() instanceof LivingEntity vehicle) {
			vehicle.getAttributes().removeAttributeModifiers(modifiers);
		}
	}
}
