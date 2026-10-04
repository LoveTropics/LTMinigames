package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.weather.tornado.PlayerController;
import org.lovetropics.games.common.core.game.weather.tornado.Tornado;

import java.util.Optional;

public record TransformPlayerTornadoAction(int timeTicks, boolean baby) implements IGameBehavior {
	public static final MapCodec<TransformPlayerTornadoAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.fieldOf("time_ticks").forGetter(c -> c.timeTicks),
			Codec.BOOL.fieldOf("baby").forGetter(c -> c.baby)
	).apply(i, TransformPlayerTornadoAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.applyToPlayers(game, (context, player) -> transformPlayer(player));
	}

	private boolean transformPlayer(ServerPlayer player) {
		Tornado tornado = new Tornado(
				Optional.of(new PlayerController(player.getUUID(), timeTicks)),
				baby,
				false,
				Optional.empty(),
				5,
				5
		);
		tornado.postAsIMCMessage(player.level());
		return true;
	}
}
