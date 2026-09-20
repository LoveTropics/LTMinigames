package org.lovetropics.games.common.core.game.behavior.instances.tweak;

import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record DamageInWaterBehavior(int interval, float amount) implements IGameBehavior {
	public static final MapCodec<DamageInWaterBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.fieldOf("interval").forGetter(DamageInWaterBehavior::interval),
			Codec.FLOAT.fieldOf("amount").forGetter(DamageInWaterBehavior::amount)
	).apply(i, DamageInWaterBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GamePlayerEvents.TICK, player -> {
			if (player.isInWater() && player.tickCount % interval == 0) {
				player.hurtServer(player.level(), player.damageSources().drown(), amount);
			}
		});
	}
}
