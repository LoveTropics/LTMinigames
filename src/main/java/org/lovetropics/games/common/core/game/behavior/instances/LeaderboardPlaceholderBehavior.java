package org.lovetropics.games.common.core.game.behavior.instances;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.placeholders.api.PlaceholderResult;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.state.statistics.LeaderboardConfig;
import org.lovetropics.games.common.core.game.util.GamePlaceholders;

import java.util.ArrayList;
import java.util.List;

public record LeaderboardPlaceholderBehavior(
		Identifier id,
		LeaderboardConfig<?> leaderboard
) implements IGameBehavior {
	public static final MapCodec<LeaderboardPlaceholderBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Identifier.CODEC.fieldOf("id").forGetter(LeaderboardPlaceholderBehavior::id),
			LeaderboardConfig.MAP_CODEC.forGetter(LeaderboardPlaceholderBehavior::leaderboard)
	).apply(i, LeaderboardPlaceholderBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		GamePlaceholders.get(game).register(id, (_, _) -> {
			List<Component> lines = new ArrayList<>(10);
			leaderboard.buildDisplay(game, lines::add);
			return PlaceholderResult.value(CommonComponents.joinLines(lines));
		});
	}
}
