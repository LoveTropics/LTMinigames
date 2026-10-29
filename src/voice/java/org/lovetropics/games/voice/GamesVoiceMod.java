package org.lovetropics.games.voice;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;
import org.lovetropics.games.voice.behavior.ModifyVoiceAction;
import org.lovetropics.games.voice.behavior.VoiceChatBehavior;
import org.slf4j.Logger;

// TODO: Ship as a separate jar, and make the dependency mandatory
@Mod(GamesVoiceMod.ID)
public class GamesVoiceMod {
	public static final String ID = "ltgames_voice";

	private static final Logger LOGGER = LogUtils.getLogger();

	public GamesVoiceMod() {
		if (!ModList.get().isLoaded("voicechat_api")) {
			LOGGER.warn("voicechat_api is not loaded, disabling");
			return;
		}
		LoveTropicsRegistrate registrate = LoveTropicsRegistrate.create(ID);
		registrate.registerBehavior("voice_chat", VoiceChatBehavior.CODEC);
		registrate.registerBehavior("modify_voice", ModifyVoiceAction.CODEC);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(ID, path);
	}
}
