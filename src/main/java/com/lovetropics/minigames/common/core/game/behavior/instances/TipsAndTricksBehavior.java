package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TipsAndTricksBehavior implements IGameBehavior {
	public static final MapCodec<TipsAndTricksBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ComponentSerialization.CODEC.listOf().fieldOf("texts").forGetter(b -> b.texts),
			Codec.INT.fieldOf("time_between_tips").forGetter(b -> b.timeBetweenTips)
	).apply(i, TipsAndTricksBehavior::new));

	private final List<Component> texts;
	private final int timeBetweenTips;

	// Mutable copy that is removed from as the game progresses
	private List<Component> remainingTexts;
	private long startTime;

	public TipsAndTricksBehavior(List<Component> texts, int timeBetweenTips) {
		this.texts = texts;
		this.timeBetweenTips = timeBetweenTips;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GamePhaseEvents.START, () -> {
			// Copy and randomize tips&tricks
			remainingTexts = new ArrayList<>(texts);
			Collections.shuffle(remainingTexts);

			startTime = game.ticks();
		});

		events.listen(GamePhaseEvents.TICK, () -> {
			if ((game.ticks() - startTime) % timeBetweenTips == 0) {
				if (!remainingTexts.isEmpty()) {
					Component text = remainingTexts.removeFirst();
					game.allPlayers().sendMessage(text);
				}
			}
		});
	}
}
