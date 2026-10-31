package org.lovetropics.games.common.core.game.persistent.behavior.crab;

import eu.pb4.placeholders.api.ArgumentParser;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.ServerPlaceholderContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.lovetropics.games.LoveTropics;

public class GolfPlaceholders {
	private static final PlaceholderResult UNKNOWN = PlaceholderResult.value("-");

	private static final int INVALID_HOLE = -1;

	private static final ArgumentParser<Integer> HOLE_PARSER = string -> {
		try {
			return string != null ? Integer.parseInt(string) : INVALID_HOLE;
		} catch (NumberFormatException e) {
			return INVALID_HOLE;
		}
	};

	public static void init() {
		registerHolePlaceholder("high_score/value", (_, golf, hole) -> {
			GolfData.Score highScore = golf.getHighScore(hole);
			int score = highScore != null ? highScore.score() : 0;
			return PlaceholderResult.value(String.valueOf(score));
		});
		registerHolePlaceholder("high_score/name", (_, golf, hole) -> {
			GolfData.Score highScore = golf.getHighScore(hole);
			if (highScore == null) {
				return UNKNOWN;
			}
			return PlaceholderResult.value(highScore.profile().name());
		});

		registerHolePlaceholder("my_score/value", (ctx, golf, hole) -> {
			ServerPlayer player = ctx.serverPlayer();
			if (player == null) {
				return UNKNOWN;
			}
			int highScore = golf.getHighScoreFor(hole, player);
			if (highScore == -1) {
				return UNKNOWN;
			}
			return PlaceholderResult.value(String.valueOf(highScore));
		});

		registerHolePlaceholder("total_plays", (_, golf, hole) -> {
			int totalPlays = golf.getTotalPlays(hole);
			if (totalPlays == -1) {
				return UNKNOWN;
			}
			return PlaceholderResult.value(String.valueOf(totalPlays));
		});
	}

	private static void registerHolePlaceholder(String name, HoleFunction function) {
		Placeholders.registerServer(LoveTropics.id("golf/" + name), HOLE_PARSER, (ctx, hole) -> {
			if (hole == INVALID_HOLE) {
				return PlaceholderResult.invalid();
			}
			ServerLevel level = ctx.serverLevel();
			if (level == null) {
				return UNKNOWN;
			}
			return function.evaluate(ctx, GolfData.get(level), hole);
		});
	}

	@FunctionalInterface
	private interface HoleFunction {
		PlaceholderResult evaluate(ServerPlaceholderContext ctx, GolfData golf, int hole);
	}
}
