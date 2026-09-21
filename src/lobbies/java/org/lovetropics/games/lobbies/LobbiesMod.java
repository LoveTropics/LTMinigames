package org.lovetropics.games.lobbies;

import org.lovetropics.games.common.core.game.IGameLookup;
import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.providers.ProviderType;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.common.Mod;

import java.util.function.BiConsumer;

@Mod(LobbiesMod.ID)
public class LobbiesMod {
	public static final String ID = "ltgames_lobbies";

	public LobbiesMod() {
		IGameLookup.Binder.bind(GamePhaseManager.get());

		Registrate registrate = Registrate.create(ID);
		registrate.addDataGenerator(ProviderType.LANG, prov -> {
			BiConsumer<String, String> output = prov::add;
			GameLobbyTexts.collectTranslations(output);
			LobbyKeybinds.TRANSLATIONS.forEach(output);
		});
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(ID, path);
	}
}
