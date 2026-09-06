package com.lovetropics.minigames.common.config;

import com.lovetropics.minigames.common.core.integration.BackendIntegrations;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public class ConfigLT {

	private static final Builder CLIENT_BUILDER = new Builder();

	private static final Builder COMMON_BUILDER = new Builder();

	public static final CategoryGeneral GENERAL = new CategoryGeneral();
	public static final CategoryIntegrations INTEGRATIONS = new CategoryIntegrations();

	public static final class CategoryGeneral {

		public final DoubleValue Precipitation_Particle_effect_rate;

		public final BooleanValue UseCrouch;

		public final IntValue donationDelay;

		public final IntValue donationPackageDelay;

		public final IntValue chatEventDelay;

		private CategoryGeneral() {
			CLIENT_BUILDER.comment("General mod settings").push("general");

			Precipitation_Particle_effect_rate = CLIENT_BUILDER
					.defineInRange("Precipitation_Particle_effect_rate", 0.7D, 0D, 1D);

			UseCrouch = CLIENT_BUILDER.comment("Enable crawling anywhere by pressing the sprint key while holding down the sneak key")
					.define("UseCrawl", true);

			donationDelay = COMMON_BUILDER
					.comment("Delay (in seconds) between donation events")
					.defineInRange("donationDelay", 2, 0, 99999);

			donationPackageDelay = COMMON_BUILDER
					.comment("Delay (in seconds) between care packages")
					.defineInRange("donationPackageDelay", 3, 0, 99999);

			chatEventDelay = COMMON_BUILDER
					.comment("Delay (in seconds) between chat events")
					.defineInRange("chatEventDelay", 1, 0, 99999);

			CLIENT_BUILDER.pop();
		}
	}

	public static final class CategoryIntegrations {

		public final ConfigValue<String> baseUrl;
		public final ConfigValue<String> worldLoadEndpoint;
		public final ConfigValue<String> worldUnloadEndpoint;
		public final ConfigValue<String> minigameStartEndpoint;
		public final ConfigValue<String> minigameEndEndpoint;
		public final ConfigValue<String> minigameCancelEndpoint;
		public final ConfigValue<String> minigamePlayerUpdateEndpoint;
		public final ConfigValue<String> minigameUpdatePackagesEndpoint;
		public final ConfigValue<String> actionResolvedEndpoint;
		public final ConfigValue<String> pendingActionsEndpoint;
		public final ConfigValue<String> authToken;
		public final ConfigValue<String> webSocketUrl;
		public final ConfigValue<String> addPollEndpoint;

		private CategoryIntegrations() {
			COMMON_BUILDER.comment("Used for the LoveTropics charity drive.").push("techStack");

			baseUrl = COMMON_BUILDER
					.comment("Base URL to use ")
					.define("baseUrl", "https://localhost:443");
			worldLoadEndpoint = COMMON_BUILDER
					.comment("Endpoint used when the server starts to reset minigame state")
					.define("worldLoadEndpoint", "minigames/minigame/worldloaded");
			worldUnloadEndpoint = COMMON_BUILDER
					.comment("Endpoint used when the server stops to reset minigame state")
					.define("worldUnloadEndpoint", "minigames/minigame/worldunloaded");
			minigameStartEndpoint = COMMON_BUILDER
					.comment("Endpoint used when a minigame is started")
					.define("minigameStartEndpoint", "minigames/minigame/start");
			minigameEndEndpoint = COMMON_BUILDER
					.comment("Endpoint used when a minigame is successfully completed")
					.define("minigameEndEndpoint", "minigames/minigame/end");
			minigameCancelEndpoint = COMMON_BUILDER
					.comment("Endpoint used when a minigame is canceled before finishing")
					.define("minigameCancelEndpoint", "minigames/minigame/cancel");
			minigamePlayerUpdateEndpoint = COMMON_BUILDER
					.comment("Endpoint used to update a player's status during a minigame")
					.define("minigamePlayerUpdateEndpoint", "minigames/minigame/playerupdate");
			minigameUpdatePackagesEndpoint = COMMON_BUILDER
					.comment("Endpoint used to update the packages used in a minigame")
					.define("minigameUpdatePackagesEndpoint", "minigames/minigame/updatepackages");
			actionResolvedEndpoint = COMMON_BUILDER
					.comment("Endpoint used to notify the backend an action was received and resolved")
					.define("actionResolvedEndpoint", "minigames/minigame/actionresolved");
			authToken = COMMON_BUILDER
					.comment("Auth token used to authenticate with the tech stack")
					.define("authToken", "");
			webSocketUrl = COMMON_BUILDER
					.comment("URL the web socket is running on")
					.define("webSocketUrl", "wss://localhost:443");
			pendingActionsEndpoint = COMMON_BUILDER
					.comment("URL to receive any care/sabotage packages or chat events that were triggered but never acknowledged by the mod (maybe due to a premature shutdown)")
					.define("pendingActionsEndpoint", "minigames/minigame/pendingactions");
			addPollEndpoint = COMMON_BUILDER
					.define("addPollEndpoint", "polls/polls/add");
			COMMON_BUILDER.pop();
		}
	}

	public static final ModConfigSpec CLIENT_CONFIG = CLIENT_BUILDER.build();
	public static final ModConfigSpec SERVER_CONFIG = COMMON_BUILDER.build();

	public static void onLoad(final ModConfigEvent.Loading event) {
		if (event.getConfig().getSpec() == SERVER_CONFIG) {
			onServerConfigLoad();
		}
	}

	/// values used during runtime that require processing from disk
	public static void onReload(final ModConfigEvent.Reloading event) {
		if (event.getConfig().getSpec() == SERVER_CONFIG) {
			onServerConfigLoad();
		}
	}

	private static void onServerConfigLoad() {
		BackendIntegrations.get().updateConfig(INTEGRATIONS.webSocketUrl.get(), INTEGRATIONS.authToken.get());
	}
}
