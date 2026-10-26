package org.lovetropics.games.voice;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartingEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.GameException;

@ForgeVoicechatPlugin
public class GamesVoicePlugin implements VoicechatPlugin {
	private static @Nullable VoicechatServerApi serverApi;

	@Override
	public String getPluginId() {
		return GamesVoiceMod.ID;
	}

	public static VoicechatServerApi voiceApi() throws GameException {
		if (serverApi == null) {
			throw new GameException(Component.literal("Voice chat not initialized"));
		}
		return serverApi;
	}

	@Override
	public void registerEvents(EventRegistration registration) {
		registration.registerEvent(VoicechatServerStartingEvent.class, event ->
				GamesVoicePlugin.serverApi = event.getVoicechat()
		);
		registration.registerEvent(VoicechatServerStoppedEvent.class, _ ->
				GamesVoicePlugin.serverApi = null
		);
		registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
	}

	private void onMicrophonePacket(MicrophonePacketEvent event) {
	}
}
