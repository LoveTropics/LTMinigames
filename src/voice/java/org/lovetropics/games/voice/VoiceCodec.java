package org.lovetropics.games.voice;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import org.jspecify.annotations.Nullable;

public class VoiceCodec implements AutoCloseable {
	private final VoicechatServerApi api;
	private @Nullable OpusDecoder decoder;
	private @Nullable OpusEncoder encoder;

	public VoiceCodec(VoicechatServerApi api) {
		this.api = api;
	}

	public short[] decode(byte[] data) {
		if (decoder == null) {
			decoder = api.createDecoder();
		}
		return decoder.decode(data);
	}

	public byte[] encode(short[] data) {
		if (encoder == null) {
			encoder = api.createEncoder();
		}
		return encoder.encode(data);
	}

	@Override
	public void close() {
		if (decoder != null) {
			decoder.close();
			decoder = null;
		}
		if (encoder != null) {
			encoder.close();
			encoder = null;
		}
	}
}
