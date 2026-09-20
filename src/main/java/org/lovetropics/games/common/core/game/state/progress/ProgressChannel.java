package org.lovetropics.games.common.core.game.state.progress;

import org.lovetropics.games.common.core.game.IGamePhase;
import com.mojang.serialization.Codec;

public record ProgressChannel(String id) {
	public static final ProgressChannel MAIN = new ProgressChannel("main");

	public static final Codec<ProgressChannel> CODEC = Codec.STRING.xmap(ProgressChannel::new, ProgressChannel::id);

	public ProgressHolder registerTo(IGamePhase game) {
		return game.state().get(GameProgressionState.KEY).register(this);
	}

	public ProgressHolder getOrThrow(IGamePhase game) {
		return game.state().get(GameProgressionState.KEY).getOrThrow(this);
	}
}
