package com.lovetropics.minigames.common.core.game.behavior.instances.tweak;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressChannel;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressionPeriod;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.TriState;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public record CancelPlayerAttacksBehavior(ProgressChannel channel, Optional<ProgressionPeriod> period) implements IGameBehavior {
	public static final MapCodec<CancelPlayerAttacksBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ProgressChannel.CODEC.optionalFieldOf("channel", ProgressChannel.MAIN).forGetter(CancelPlayerAttacksBehavior::channel),
			ProgressionPeriod.CODEC.optionalFieldOf("period").forGetter(CancelPlayerAttacksBehavior::period)
	).apply(i, CancelPlayerAttacksBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		BooleanSupplier predicate = period.map(p -> p.createPredicate(game, channel)).orElse(() -> true);
		events.listen(GamePlayerEvents.ATTACK, (player, target) -> predicate.getAsBoolean() ? TriState.FALSE : TriState.DEFAULT);
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.CANCEL_PLAYER_ATTACKS;
	}
}
