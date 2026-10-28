package org.lovetropics.games.voice.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.state.GameStateKey;
import org.lovetropics.games.common.core.game.state.IGameState;
import org.lovetropics.games.voice.GameVoiceEvents;
import org.lovetropics.games.voice.VoiceModifiers;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.UnaryOperator;

public record ModifyVoiceAction(
		Optional<Float> volume,
		Optional<Float> quality
) implements IGameBehavior {
	public static final MapCodec<ModifyVoiceAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.floatRange(0.0f, 2.0f).optionalFieldOf("volume").forGetter(ModifyVoiceAction::volume),
			Codec.floatRange(0.01f, 1.0f).optionalFieldOf("quality").forGetter(ModifyVoiceAction::quality)
	).apply(i, ModifyVoiceAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		SharedState sharedState = SharedState.getOrAdd(game, events);
		events.applyToPlayers(game, (_, target) -> {
			sharedState.update(target, oldModifiers -> new VoiceModifiers(
					volume.orElse(oldModifiers.volume()),
					quality.orElse(oldModifiers.quality())
			));
			return true;
		});
	}

	private static class SharedState implements IGameState {
		public static final GameStateKey<SharedState> KEY = GameStateKey.create("Voice modifiers");

		// Modified only by main thread, read off-thread
		private final ConcurrentMap<UUID, VoiceModifiers> modifiersByPlayer = new ConcurrentHashMap<>();

		public static SharedState getOrAdd(IGamePhase game, EventRegistrar events) {
			SharedState state = game.state().getOrNull(KEY);
			if (state != null) {
				return state;
			}
			SharedState newState = new SharedState();
			// Warning: called off-thread
			events.listen(GameVoiceEvents.MICROPHONE, (playerId, event, codec) -> {
				VoiceModifiers modifiers = newState.modifiersByPlayer.get(playerId);
				if (modifiers != null) {
					modifiers.apply(event, codec);
				}
			});
			events.listen(GamePlayerEvents.REMOVE, player ->
					newState.modifiersByPlayer.remove(player.getUUID())
			);
			game.state().register(KEY, newState);
			return newState;
		}

		public void update(ServerPlayer player, UnaryOperator<VoiceModifiers> updater) {
			modifiersByPlayer.compute(player.getUUID(), (_, modifiers) -> {
				VoiceModifiers oldModifiers = Objects.requireNonNullElse(modifiers, VoiceModifiers.DEFAULT);
				VoiceModifiers newModifiers = updater.apply(oldModifiers);
				return newModifiers.equals(VoiceModifiers.DEFAULT) ? null : newModifiers;
			});
		}
	}
}
