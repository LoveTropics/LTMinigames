package com.lovetropics.minigames.common.content.river_race.behaviour;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.function.Supplier;

public record KillInVoidBehavior(String voidBelowRegionKey) implements IGameBehavior {
	public static final MapCodec<KillInVoidBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("void_below_region").forGetter(KillInVoidBehavior::voidBelowRegionKey)
	).apply(i, KillInVoidBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		BlockBox voidRegion = game.mapRegions().getOrThrow(voidBelowRegionKey);
		int voidY = voidRegion.min().getY();
		events.listen(GamePlayerEvents.TICK, player -> {
			if (player.isSpectator() || player.isCreative()) {
				return;
			}
			if (player.getY() < voidY) {
				player.kill(game.level());
			}
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.KILL_IN_VOID;
	}
}
