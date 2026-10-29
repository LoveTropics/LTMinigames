package org.lovetropics.games.common.core.game.behavior.instances.tweak;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.player.Player;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.state.progress.ProgressChannel;
import org.lovetropics.games.common.core.game.state.progress.ProgressionPeriod;

import java.util.Optional;
import java.util.function.BooleanSupplier;

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
	public MapCodec<? extends IGameBehavior> codec() {
		return CODEC;
	}
}
