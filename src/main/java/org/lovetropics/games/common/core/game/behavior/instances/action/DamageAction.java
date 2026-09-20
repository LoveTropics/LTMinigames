package org.lovetropics.games.common.core.game.behavior.instances.action;

import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

import java.util.Optional;

public record DamageAction(Optional<Holder<DamageType>> source, float amount) implements IGameBehavior {
	public static final MapCodec<DamageAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			DamageType.CODEC.optionalFieldOf("source").forGetter(DamageAction::source),
			Codec.FLOAT.fieldOf("amount").forGetter(DamageAction::amount)
	).apply(i, DamageAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.applyToEntities(game, (context, level, entity) -> {
			entity.hurtServer(
					level,
					source.map(DamageSource::new)
							.orElseGet(entity.damageSources()::generic),
					amount
			);
			return true;
		});
	}
}
