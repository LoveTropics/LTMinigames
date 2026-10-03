package org.lovetropics.games.common.config;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.integration.BackendIntegrations;

@EventBusSubscriber(modid = LoveTropics.ID)
public class ConfigLT {

	private static final Builder CLIENT_BUILDER = new Builder();

	private static final Builder COMMON_BUILDER = new Builder();

	public static final CategoryGeneral GENERAL = new CategoryGeneral();
	public static final CategoryIntegrations INTEGRATIONS = new CategoryIntegrations();

	public static final class CategoryGeneral {

		public final IntValue donationDelay;

		public final IntValue donationPackageDelay;

		public final IntValue chatEventDelay;

		public final BooleanValue skipIntroSlideshows;

		private CategoryGeneral() {
			CLIENT_BUILDER.comment("General mod settings").push("general");

			donationDelay = COMMON_BUILDER
					.comment("Delay (in seconds) between donation events")
					.defineInRange("donationDelay", 2, 0, 99999);

			donationPackageDelay = COMMON_BUILDER
					.comment("Delay (in seconds) between care packages")
					.defineInRange("donationPackageDelay", 3, 0, 99999);

			chatEventDelay = COMMON_BUILDER
					.comment("Delay (in seconds) between chat events")
					.defineInRange("chatEventDelay", 1, 0, 99999);

			skipIntroSlideshows = COMMON_BUILDER
					.comment("If true, minigame introduction slideshows will be skipped")
					.define("skipIntroSlideshows", false);

			CLIENT_BUILDER.pop();
		}
	}

	public static final class CategoryIntegrations {
		public final BooleanValue enabled;
		public final ConfigValue<String> minigamesServiceUrl;
		public final ConfigValue<String> pollsServiceUrl;
		public final ConfigValue<String> webSocketUrl;
		public final ConfigValue<String> authToken;

		public final BooleanValue alwaysPublishGames;

		private CategoryIntegrations() {
			COMMON_BUILDER.comment("Used for the LoveTropics charity drive.").push("techStack");

			enabled = COMMON_BUILDER
					.comment("If the TechStack connection should be enabled")
					.define("enabled", false);

			minigamesServiceUrl = COMMON_BUILDER
					.comment("URL of the minigames service")
					.define("minigamesServiceUrl", "http://api.localhost:80/minigames/minigame");
			pollsServiceUrl = COMMON_BUILDER
					.comment("URL of the polls service")
					.define("pollsServiceUrl", "https://api.localhost:80/polls/polls");
			webSocketUrl = COMMON_BUILDER
					.comment("URL the web socket is running on")
					.define("webSocketUrl", "ws://api.localhost:80");
			authToken = COMMON_BUILDER
					.comment("Auth token used to authenticate with the tech stack")
					.define("authToken", "sekrit");

			alwaysPublishGames = COMMON_BUILDER
					.comment("If true, lobbies will always be focused live")
					.define("alwaysPublishGames", false);

			COMMON_BUILDER.pop();
		}
	}

	public static final ModConfigSpec CLIENT_CONFIG = CLIENT_BUILDER.build();
	public static final ModConfigSpec SERVER_CONFIG = COMMON_BUILDER.build();

	@SubscribeEvent
	public static void onLoad(ModConfigEvent.Loading event) {
		if (event.getConfig().getSpec() == SERVER_CONFIG) {
			onServerConfigLoad();
		}
	}

	/// values used during runtime that require processing from disk
	@SubscribeEvent
	public static void onReload(ModConfigEvent.Reloading event) {
		if (event.getConfig().getSpec() == SERVER_CONFIG) {
			onServerConfigLoad();
		}
	}

	private static void onServerConfigLoad() {
		if (INTEGRATIONS.enabled.get()) {
			BackendIntegrations.get().updateConfig(INTEGRATIONS.webSocketUrl.get(), INTEGRATIONS.authToken.get());
		} else {
			BackendIntegrations.get().clearConfig();
		}
	}
}
