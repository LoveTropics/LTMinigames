package com.lovetropics.minigames.common.core.game.behavior.instances.tweak;

import com.lovetropics.minigames.SoundRegistry;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameWorldEvents;
import com.mojang.serialization.MapCodec;

public record QuietExplosionsBehavior() implements IGameBehavior {
	public static final MapCodec<QuietExplosionsBehavior> CODEC = MapCodec.unit(QuietExplosionsBehavior::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameWorldEvents.EXPLOSION_SOUND, (explosion, sound) -> SoundRegistry.QUIET_EXPLOSION);
	}
}
