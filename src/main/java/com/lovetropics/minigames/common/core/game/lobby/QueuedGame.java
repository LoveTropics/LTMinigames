package com.lovetropics.minigames.common.core.game.lobby;

import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.IGamePhaseDefinition;
import com.lovetropics.minigames.common.core.game.behavior.BehaviorList;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Essentially the data underlying a queued game which is stored in a game lobby
 */
public record QueuedGame(int networkId, IGameDefinition definition, BehaviorList playingBehaviors, @Nullable BehaviorList waitingBehaviors) {
	private static final AtomicInteger NEXT_NETWORK_ID = new AtomicInteger();

	public QueuedGame(int networkId, IGameDefinition definition, BehaviorList playingBehaviors, BehaviorList waitingBehaviors) {
		this.networkId = networkId;
		this.definition = definition;
		this.playingBehaviors = playingBehaviors;
		this.waitingBehaviors = waitingBehaviors;
	}

	public static QueuedGame create(IGameDefinition game) {
		BehaviorList playingBehaviors = game.getPlayingPhase().createBehaviors();
		BehaviorList waitingBehaviors = game.getWaitingPhase().map(IGamePhaseDefinition::createBehaviors).orElse(BehaviorList.EMPTY);
		return new QueuedGame(NEXT_NETWORK_ID.getAndIncrement(), game, playingBehaviors, waitingBehaviors);
	}
}
