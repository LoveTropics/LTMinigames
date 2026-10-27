package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;
import org.lovetropics.games.common.core.game.weather.tornado.Tornado;

public record SpawnTornadoAction(Tornado tornado) implements IGameBehavior {
	public static final MapCodec<SpawnTornadoAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Tornado.CODEC.fieldOf("tornado").forGetter(SpawnTornadoAction::tornado)
	).apply(i, SpawnTornadoAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			// We should probably reconsider at some point if this is a reasonable way to spawn them across levels
			for (ServerLevel level : game.allLevels()) {
				if (!game.allPlayers().inLevel(level).isEmpty()) {
					tornado.postAsIMCMessage(level);
				}
			}
			return true;
		});
	}
}
