package org.lovetropics.games.voice;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartingEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGameLookup;
import org.lovetropics.games.common.core.game.IGamePhase;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ForgeVoicechatPlugin
public class GamesVoicePlugin implements VoicechatPlugin {
	private static @Nullable VoicechatServerApi serverApi;

	private static final @Nullable VoiceModifiers DEBUG_MODIFIERS = null;

	private final Map<UUID, VoiceCodec> voiceCodecs = new HashMap<>();

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
		registration.registerEvent(VoicechatServerStoppedEvent.class, _ -> {
			GamesVoicePlugin.serverApi = null;
			voiceCodecs.values().forEach(VoiceCodec::close);
			voiceCodecs.clear();
		});
		registration.registerEvent(PlayerDisconnectedEvent.class, event -> {
			VoiceCodec voiceCodec = voiceCodecs.remove(event.getPlayerUuid());
			if (voiceCodec != null) {
				voiceCodec.close();
			}
		});
		registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
	}

	private void onMicrophonePacket(MicrophonePacketEvent event) {
		ServerPlayer sender = event.getSenderConnection().getPlayer();
		VoiceCodec codec = voiceCodecs.computeIfAbsent(sender.getUuid(), _ -> new VoiceCodec(event.getVoicechat()));

		IGamePhase game = IGameLookup.get().getGamePhaseFor((Player) sender.getPlayer());
		if (game != null) {
			game.invoker(GameVoiceEvents.MICROPHONE).onMicrophonePacket(sender.getUuid(), event, codec);
		}

		if (DEBUG_MODIFIERS != null) {
			VoicechatServerApi api = event.getVoicechat();
			DEBUG_MODIFIERS.apply(event, codec);
			api.sendStaticSoundPacketTo(event.getSenderConnection(), event.getPacket().staticSoundPacketBuilder().build());
		}
	}
}
