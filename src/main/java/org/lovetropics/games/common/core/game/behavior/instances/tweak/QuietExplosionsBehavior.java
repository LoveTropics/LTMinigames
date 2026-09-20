package org.lovetropics.games.common.core.game.behavior.instances.tweak;

import org.lovetropics.games.SoundRegistry;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameWorldEvents;
import com.mojang.serialization.MapCodec;

public record QuietExplosionsBehavior() implements IGameBehavior {
	public static final MapCodec<QuietExplosionsBehavior> CODEC = MapCodec.unit(QuietExplosionsBehavior::new);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameWorldEvents.EXPLOSION_SOUND, (level, explosion, sound) -> SoundRegistry.QUIET_EXPLOSION);
	}
}
