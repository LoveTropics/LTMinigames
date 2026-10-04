package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.weather.tornado.Tornado;

public record SpawnTornadoAction(Tornado tornado) implements IGameBehavior {
	public static final MapCodec<SpawnTornadoAction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
					Tornado.CODEC.fieldOf("tornado").forGetter(SpawnTornadoAction::tornado)
			).apply(instance, SpawnTornadoAction::new)
	);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameActionEvents.APPLY, (context, targets) -> spawnTornado(game));
	}

	private boolean spawnTornado(IGamePhase game) {
		tornado.postAsIMCMessage(game.level());
		return true;
	}

}
