package com.lovetropics.minigames.common.core.game.behavior.instances.tweak;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.criterion.DamageSourcePredicate;

public record ScaleDamageBehavior(float factor, DamageSourcePredicate source) implements IGameBehavior {
	public static final MapCodec<ScaleDamageBehavior> CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
			Codec.floatRange(0.0F, Float.MAX_VALUE).fieldOf("factor").forGetter(ScaleDamageBehavior::factor),
			DamageSourcePredicate.CODEC.fieldOf("source").forGetter(ScaleDamageBehavior::source)
	).apply(in, ScaleDamageBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GamePlayerEvents.DAMAGE_AMOUNT, (player, damageSource, amount, originalAmount) -> {
			if (source.matches(player, damageSource)) {
				return factor * amount;
			}
			return amount;
		});
	}
}
