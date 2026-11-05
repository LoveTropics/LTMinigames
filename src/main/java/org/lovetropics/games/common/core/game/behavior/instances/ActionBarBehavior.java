package org.lovetropics.games.common.core.game.behavior.instances;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.mutable.MutableInt;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.util.TemplatedText;

import java.util.Map;

public record ActionBarBehavior(
		TemplatedText text
) implements IGameBehavior {
	public static final MapCodec<ActionBarBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			TemplatedText.CODEC.fieldOf("text").forGetter(ActionBarBehavior::text)
	).apply(i, ActionBarBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		MutableInt totalParticipants = new MutableInt();
		events.listen(GamePlayerEvents.BEFORE_ADD_PLAYERS, (participants, spectators) ->
				totalParticipants.setValue(participants.size())
		);

		events.listen(GamePlayerEvents.TICK, player -> {
			if (player.tickCount % 10 == 0) {
				player.sendSystemMessage(text.apply(Map.of(
						"participants", Component.literal(String.valueOf(game.participants().size())),
						"total_participants", Component.literal(totalParticipants.toString())
				)), true);
			}
		});
	}
}
