package org.lovetropics.games.common.core.game.persistent.behavior.crab;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.LoveTropics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GolfData extends SavedData {
	private static final Identifier ID = LoveTropics.id("crab_golf_data");

	public static final Codec<GolfData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.unboundedMap(Codec.STRING.xmap(Integer::parseInt, Object::toString), HoleData.CODEC).fieldOf("data").forGetter(b -> b.data)
	).apply(instance, GolfData::new));

	private static final SavedDataType<GolfData> TYPE = new SavedDataType<>(ID, GolfData::new, CODEC);

	private final Map<Integer, HoleData> data;

	private GolfData() {
		this(new HashMap<>());
	}

	private GolfData(Map<Integer, HoleData> data) {
		this.data = new HashMap<>(data);
	}

	public @Nullable Score getHighScore(int hole) {
		HoleData d = data.get(hole);
		if (d == null) {
			return null;
		}
		return d.highScore;
	}

	public void clearHole(int hole) {
		data.remove(hole);
		setDirty();
	}

	public int getHighScoreFor(int hole, ServerPlayer player) {
		HoleData d = data.get(hole);
		if (d == null) {
			return -1;
		}
		Score score = d.playerHighScores.get(player.nameAndId());
		return score == null ? -1 : score.score;
	}

	public int getTotalPlays(int hole) {
		HoleData d = data.get(hole);
		return d != null ? d.totalPlays : 0;
	}

	// Returns whether this is a high score
	public boolean submitNewScore(int hole, ServerPlayer player, int score) {
		if (score == -1) {
			return false;
		}

		Score playerScore = new Score(player.nameAndId(), score);

		HoleData d = data.get(hole);
		if (d == null) {
			HoleData hdata = new HoleData(playerScore, 1, Map.of(player.nameAndId(), playerScore));
			data.put(hole, hdata);
			setDirty();
			return true;
		}

		boolean betterEquals = score >= d.highScore.score;
		if (betterEquals) {
			d.highScore = playerScore;
		}
		d.totalPlays++;
		d.playerHighScores.compute(player.nameAndId(), (_, oldScore) -> {
			if (oldScore == null || score > oldScore.score) {
				return playerScore;
			}
			return oldScore;
		});
		setDirty();

		return betterEquals;
	}

	public static GolfData get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	private static final class HoleData {
		private static final Codec<Map<NameAndId, Score>> SCORES_CODEC = Score.CODEC.listOf().xmap(
				scores -> scores.stream().collect(Collectors.toMap(Score::profile, o -> o)),
				map -> List.copyOf(map.values())
		);

		public static final Codec<HoleData> CODEC = RecordCodecBuilder.create(i -> i.group(
				Score.CODEC.fieldOf("high_score").forGetter(d -> d.highScore),
				Codec.INT.fieldOf("total_plays").forGetter(d -> d.totalPlays),
				SCORES_CODEC.fieldOf("player_scores").forGetter(d -> d.playerHighScores)
		).apply(i, HoleData::new));

		private Score highScore;
		private int totalPlays;
		private final Map<NameAndId, Score> playerHighScores;

		public HoleData(Score highScore, int totalPlays, Map<NameAndId, Score> playerHighScores) {
			this.highScore = highScore;
			this.totalPlays = totalPlays;
			this.playerHighScores = new HashMap<>(playerHighScores);
		}
	}

	public record Score(
			NameAndId profile,
			int score
	) {
		public static final Codec<Score> CODEC = RecordCodecBuilder.create(i -> i.group(
				NameAndId.CODEC.fieldOf("profile").forGetter(Score::profile),
				Codec.INT.fieldOf("score").forGetter(Score::score)
		).apply(i, Score::new));
	}
}
