package org.lovetropics.games.lobbies;

import org.lovetropics.games.common.core.game.IGameLookup;
import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.providers.ProviderType;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.common.Mod;

@Mod(LobbiesMod.ID)
public class LobbiesMod {
	public static final String ID = "ltgames_lobbies";

	public LobbiesMod() {
		IGameLookup.Binder.bind(GamePhaseManager.get());

		Registrate registrate = Registrate.create(ID);
		registrate.addDataGenerator(ProviderType.LANG, prov ->
				GameLobbyTexts.collectTranslations(prov::add)
		);
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(ID, path);
	}
}
