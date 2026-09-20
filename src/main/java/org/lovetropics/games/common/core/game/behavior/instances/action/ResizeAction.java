package org.lovetropics.games.common.core.game.behavior.instances.action;

import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import org.lovetropics.peekaboo.api.Disguise;
import org.lovetropics.peekaboo.api.EntityDisguiseHolder;

import java.util.Objects;

public record ResizeAction(
		double multiplier
) implements IGameBehavior {
	public static final MapCodec<ResizeAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.DOUBLE.fieldOf("multiplier").forGetter(ResizeAction::multiplier)
	).apply(i, ResizeAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.applyToPlayers(game, (context, target) -> {
			EntityDisguiseHolder holder = EntityDisguiseHolder.getOrNull(target);
			if (holder != null) {
				Disguise disguise = Objects.requireNonNullElse(holder.disguise(), Disguise.NONE);
				holder.set(disguise.withScale((float) Mth.clamp(disguise.scale() * multiplier, 0.1f, 20.0f)));
				return true;
			}
			return false;
		});
	}
}
