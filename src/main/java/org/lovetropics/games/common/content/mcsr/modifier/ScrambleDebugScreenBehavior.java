package org.lovetropics.games.common.content.mcsr.modifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import org.lovetropics.games.common.core.game.client_state.instance.ScrambleDebugScreenClientState;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/// Scrambles the debug screen (F3) of the participants more and more the stronger the modifier is: first by shuffling its
/// lines, then by moving them between both sides, while slipping in fake lines and garbling its numbers
///
/// @param shuffleLevel             the level from which lines are shuffled
/// @param swapSidesLevel           the level from which lines move between both sides
/// @param rescrambleChancePerLevel how much more likely the screen is to be scrambled a new way when it is opened, for each level
/// @param extraLines               lines that don't belong on the screen at all, shown from a given level
public record ScrambleDebugScreenBehavior(String modifier, int shuffleLevel, int swapSidesLevel, float fakeLinesPerLevel, float garbleChancePerLevel, float rescrambleChancePerLevel, List<ExtraLine> extraLines) implements IGameBehavior {
	public static final MapCodec<ScrambleDebugScreenBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("modifier").forGetter(ScrambleDebugScreenBehavior::modifier),
			ExtraCodecs.POSITIVE_INT.optionalFieldOf("shuffle_level", 1).forGetter(ScrambleDebugScreenBehavior::shuffleLevel),
			ExtraCodecs.POSITIVE_INT.optionalFieldOf("swap_sides_level", 3).forGetter(ScrambleDebugScreenBehavior::swapSidesLevel),
			ExtraCodecs.NON_NEGATIVE_FLOAT.optionalFieldOf("fake_lines_per_level", 0.5f).forGetter(ScrambleDebugScreenBehavior::fakeLinesPerLevel),
			ExtraCodecs.NON_NEGATIVE_FLOAT.optionalFieldOf("garble_chance_per_level", 0.05f).forGetter(ScrambleDebugScreenBehavior::garbleChancePerLevel),
			ExtraCodecs.NON_NEGATIVE_FLOAT.optionalFieldOf("rescramble_chance_per_level", 0.1f).forGetter(ScrambleDebugScreenBehavior::rescrambleChancePerLevel),
			ExtraLine.CODEC.listOf().optionalFieldOf("extra_lines", List.of()).forGetter(ScrambleDebugScreenBehavior::extraLines)
	).apply(i, ScrambleDebugScreenBehavior::new));

	private static final int UPDATE_INTERVAL = 10;

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		GameModifiers modifiers = GameModifiers.getOrThrow(game, modifier);
		// What each participant of this phase was last sent, to only send it again when the level changes
		Map<UUID, ScrambleDebugScreenClientState> sent = new HashMap<>();

		events.listen(GamePhaseEvents.TICK, () -> {
			if (game.ticks() % UPDATE_INTERVAL != 0) {
				return;
			}
			ScrambleDebugScreenClientState state = createState(modifiers.level(modifier));
			for (ServerPlayer player : game.participants()) {
				if (Objects.equals(sent.get(player.getUUID()), state)) {
					continue;
				}
				if (state != null) {
					sent.put(player.getUUID(), state);
					GameClientState.sendToPlayer(state, player);
				} else {
					sent.remove(player.getUUID());
					GameClientState.removeFromPlayer(GameClientStateTypes.SCRAMBLE_DEBUG_SCREEN.get(), player);
				}
			}
		});
		events.listen(GamePlayerEvents.REMOVE, player -> {
			if (sent.remove(player.getUUID()) != null) {
				GameClientState.removeFromPlayer(GameClientStateTypes.SCRAMBLE_DEBUG_SCREEN.get(), player);
			}
		});
	}

	private @Nullable ScrambleDebugScreenClientState createState(int level) {
		if (level <= 0) {
			return null;
		}
		return new ScrambleDebugScreenClientState(
				level >= shuffleLevel,
				level >= swapSidesLevel,
				(int) Math.floor(level * fakeLinesPerLevel),
				Math.min(1.0f, level * garbleChancePerLevel),
				Math.min(1.0f, level * rescrambleChancePerLevel),
				getExtraLines(level)
		);
	}

	/// Extra lines about the same thing replace each other, e.g. `Scope: Normal` with `Scope: Expanded` at a higher level
	private List<String> getExtraLines(int level) {
		Map<String, ExtraLine> linesByKey = new LinkedHashMap<>();
		for (ExtraLine line : extraLines) {
			if (level >= line.level) {
				linesByKey.merge(line.key(), line, (a, b) -> b.level >= a.level ? b : a);
			}
		}
		return linesByKey.values().stream().map(ExtraLine::line).toList();
	}

	/// @param level the level from which the line is shown
	public record ExtraLine(int level, String line) {
		public static final Codec<ExtraLine> CODEC = RecordCodecBuilder.create(i -> i.group(
				ExtraCodecs.POSITIVE_INT.fieldOf("level").forGetter(ExtraLine::level),
				Codec.STRING.fieldOf("line").forGetter(ExtraLine::line)
		).apply(i, ExtraLine::new));

		/// What the line is about: what comes before its `:`
		private String key() {
			int end = line.indexOf(':');
			return end >= 0 ? line.substring(0, end) : line;
		}
	}
}
