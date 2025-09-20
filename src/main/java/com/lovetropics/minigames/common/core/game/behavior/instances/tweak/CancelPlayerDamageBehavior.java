package com.lovetropics.minigames.common.core.game.behavior.instances.tweak;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressChannel;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressionPeriod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public record CancelPlayerDamageBehavior(boolean knockback, ProgressChannel channel, Optional<ProgressionPeriod> period) implements IGameBehavior {
	public static final MapCodec<CancelPlayerDamageBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.BOOL.optionalFieldOf("knockback", false).forGetter(CancelPlayerDamageBehavior::knockback),
			ProgressChannel.CODEC.optionalFieldOf("channel", ProgressChannel.MAIN).forGetter(CancelPlayerDamageBehavior::channel),
			ProgressionPeriod.CODEC.optionalFieldOf("period").forGetter(CancelPlayerDamageBehavior::period)
	).apply(i, CancelPlayerDamageBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		BooleanSupplier predicate = period.map(p -> p.createPredicate(game, channel)).orElse(() -> true);
		if (knockback) {
			events.listen(GamePlayerEvents.DAMAGE_AMOUNT, (player, damageSource, amount, originalAmount) -> predicate.getAsBoolean() ? 0.0F : amount);
		} else {
			events.listen(GamePlayerEvents.ATTACK, (player, target) -> target instanceof Player && predicate.getAsBoolean() ? TriState.FALSE : TriState.DEFAULT);
			events.listen(GamePlayerEvents.DAMAGE, (player, damageSource, amount) -> predicate.getAsBoolean() ? TriState.FALSE : TriState.DEFAULT);
		}
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.CANCEL_PLAYER_DAMAGE;
	}
}
