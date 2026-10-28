package org.lovetropics.games.voice;

import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import net.minecraft.util.Mth;

public record VoiceModifiers(
		float volume,
		float quality
) {
	public static final VoiceModifiers DEFAULT = new VoiceModifiers(1.0f, 1.0f);

	public void apply(MicrophonePacketEvent event, VoiceCodec codec) {
		if (volume == 0.0f) {
			event.cancel();
			return;
		}
		short[] samples = codec.decode(event.getPacket().getOpusEncodedData());
		apply(samples);
		event.getPacket().setOpusEncodedData(codec.encode(samples));
	}

	private void apply(short[] samples) {
		if (volume != 1.0f) {
			for (int i = 0; i < samples.length; i++) {
				samples[i] = (short) (samples[i] * volume);
			}
		}
		// Super silly and naive, but whatever!
		if (quality < 1.0f) {
			float step = 0.0f;
			int sampleIndex = 0;
			for (int i = 0; i < samples.length; i++) {
				if (step >= 1.0f) {
					step = Mth.frac(step);
					sampleIndex = i;
				}
				samples[i] = samples[sampleIndex];
				step += quality;
			}
		}
	}
}
