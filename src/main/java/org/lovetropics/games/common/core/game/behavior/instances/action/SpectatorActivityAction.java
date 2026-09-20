package org.lovetropics.games.common.core.game.behavior.instances.action;

import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.network.SpectatorPlayerActivityMessage;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.TextColor;

public record SpectatorActivityAction(TextColor style) implements IGameBehavior {
	public static final MapCodec<SpectatorActivityAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			TextColor.CODEC.fieldOf("style").forGetter(SpectatorActivityAction::style)
	).apply(i, SpectatorActivityAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.applyToPlayers(game, (context, target) -> {
			game.spectators().sendPacket(new SpectatorPlayerActivityMessage(target.getUUID(), style.getValue()));
			return true;
		});
	}
}
