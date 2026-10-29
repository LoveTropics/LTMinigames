package org.lovetropics.games.common.core.game.behavior.instances.tweak;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.TriState;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.state.progress.ProgressChannel;
import org.lovetropics.games.common.core.game.state.progress.ProgressionPeriod;

import java.util.Optional;
import java.util.function.BooleanSupplier;

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
	public MapCodec<? extends IGameBehavior> codec() {
		return CODEC;
	}
}
