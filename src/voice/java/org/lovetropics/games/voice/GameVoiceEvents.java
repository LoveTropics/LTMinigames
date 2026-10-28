package org.lovetropics.games.voice;

import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import org.lovetropics.games.common.core.game.behavior.event.GameEventType;

import java.util.UUID;

/// Warning: all events may be invoked from off-thread.
public class GameVoiceEvents {
	public static final GameEventType<MicrophoneEvent> MICROPHONE = GameEventType.create(MicrophoneEvent.class, listeners -> (speakerId, event, codec) -> {
		for (MicrophoneEvent listener : listeners) {
			listener.onMicrophonePacket(speakerId, event, codec);
		}
	});

	public interface MicrophoneEvent {
		void onMicrophonePacket(UUID speakerId, MicrophonePacketEvent event, VoiceCodec codec);
	}
}
