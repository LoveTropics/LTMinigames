package org.lovetropics.games.common.core.game.client_state.instance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;

import java.util.List;

/// Messes with the debug screen (F3) of the player, so that it can't be trusted
///
/// @param shuffle      whether the lines are shown in a random order
/// @param swapSides    whether lines move between the left and right side of the screen
/// @param fakeLines    how many made up lines are slipped in, such as a wrong position
/// @param garbleChance     the chance for every number on the screen to be replaced with a random one
/// @param rescrambleChance the chance for the screen to be scrambled a new way every time the player opens it
/// @param extraLines       lines that don't belong on the screen at all, which are always shown as they are
public record ScrambleDebugScreenClientState(boolean shuffle, boolean swapSides, int fakeLines, float garbleChance, float rescrambleChance, List<String> extraLines) implements GameClientState {
	public static final MapCodec<ScrambleDebugScreenClientState> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.BOOL.optionalFieldOf("shuffle", false).forGetter(ScrambleDebugScreenClientState::shuffle),
			Codec.BOOL.optionalFieldOf("swap_sides", false).forGetter(ScrambleDebugScreenClientState::swapSides),
			ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("fake_lines", 0).forGetter(ScrambleDebugScreenClientState::fakeLines),
			Codec.floatRange(0.0f, 1.0f).optionalFieldOf("garble_chance", 0.0f).forGetter(ScrambleDebugScreenClientState::garbleChance),
			Codec.floatRange(0.0f, 1.0f).optionalFieldOf("rescramble_chance", 1.0f).forGetter(ScrambleDebugScreenClientState::rescrambleChance),
			Codec.STRING.listOf().optionalFieldOf("extra_lines", List.of()).forGetter(ScrambleDebugScreenClientState::extraLines)
	).apply(i, ScrambleDebugScreenClientState::new));

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.SCRAMBLE_DEBUG_SCREEN.get();
	}
}
