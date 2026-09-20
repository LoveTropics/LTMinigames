package org.lovetropics.games.common.content.river_race.microgames;

import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGameDefinition;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.player.PlayerSet;
import org.lovetropics.games.common.core.game.state.GameStateMap;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextMap;

public record MicrogamesBehavior(
		GameActionList onComplete
) implements IGameBehavior {
	public static final MapCodec<MicrogamesBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			GameActionList.CODEC.optionalFieldOf("on_complete", GameActionList.EMPTY).forGetter(MicrogamesBehavior::onComplete)
	).apply(i, MicrogamesBehavior::new));

	@Override
	public void registerState(IGamePhase game, GameStateMap phaseState, GameStateMap instanceState) {
		instanceState.register(MicrogamesManager.KEY, new MicrogamesManager(game));
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		onComplete.register(game, events);

		MicrogamesManager microgames = MicrogamesManager.get(game);
		microgames.register(events);

		events.listen(MicrogameEvents.CREATE_MICROGAME, (subGame, subEvents) -> {
			subEvents.listen(GamePlayerEvents.ADD, player ->
					onPlayerJoinMicrogame(subGame, player)
			);
		});

		events.listen(MicrogameEvents.MICROGAMES_ENDED, () ->
				onComplete.apply(game, ContextMap.EMPTY, ActionSubjects.EMPTY)
		);
	}

	private void onPlayerJoinMicrogame(IGamePhase subGame, ServerPlayer player) {
		IGameDefinition definition = subGame.definition();
		player.sendSystemMessage(Component.literal("Now Playing: ").append(definition.name()).withStyle(ChatFormatting.GREEN));
		PlayerSet.of(player).showTitle(Component.empty().append(definition.name()).withStyle(ChatFormatting.GREEN), definition.subtitle(), 10, 40, 10);
	}
}
