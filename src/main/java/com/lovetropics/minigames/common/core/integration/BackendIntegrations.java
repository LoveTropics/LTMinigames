package com.lovetropics.minigames.common.core.integration;

import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lovetropics.lib.backend.BackendConnection;
import com.lovetropics.lib.backend.BackendConnectionConfig;
import com.lovetropics.lib.backend.BackendProxy;
import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.config.ConfigLT;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.state.GameStateMap;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@EventBusSubscriber(modid = LoveTropics.ID)
public final class BackendIntegrations {
	public static final boolean DEBUG_LOGGING_BACKEND = false;

	private static final Supplier<BackendIntegrations> INSTANCE = Suppliers.memoize(BackendIntegrations::new);

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final int RETRY_DELAY_SECONDS = 30;
	private static final int MAX_RETRIES = 5;

	private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(
			new ThreadFactoryBuilder()
					.setNameFormat("lt-integrations")
					.setDaemon(true)
					.build()
	);

	private final IntegrationSender sender = DEBUG_LOGGING_BACKEND ? IntegrationSender.LOGGING : IntegrationSender.open();
	private final IntegrationSender pollSender = DEBUG_LOGGING_BACKEND ? IntegrationSender.LOGGING : IntegrationSender.openPoll();

	private final BackendProxy proxy;

	@Nullable
	private GameInstanceIntegrations liveInstance;

	private BackendIntegrations() {
		proxy = new BackendProxy(new BackendConnection.Handler() {
			@Override
			public void acceptOpened() {
			}

			@Override
			public void acceptMessage(JsonObject payload) {
				handlePayload(payload);
			}

			@Override
			public void acceptError(Throwable cause) {
				LOGGER.error("Integrations websocket closed with error: {}", cause.getMessage());
			}

			@Override
			public void acceptClosed(int code, @Nullable String reason) {
				LOGGER.error("Integrations websocket closed with code: {} and reason: {}", code, reason);
			}
		});
	}

	public static BackendIntegrations get() {
		return INSTANCE.get();
	}

	@Nullable
	private static BackendConnectionConfig connectionConfig() {
		ConfigLT.CategoryIntegrations integrations = ConfigLT.INTEGRATIONS;
		if (!integrations.isEnabled()) {
			return null;
		}

		try {
			BackendConnectionConfig config = BackendConnectionConfig.of(new URI(integrations.webSocketUrl.get()));
			String token = integrations.authToken.get();
			if (!token.isBlank()) {
				config = config.withToken(token);
			}
			return config.withSubscriptions(GameInstanceIntegrations.SUBSCRIPTIONS);
		} catch (URISyntaxException e) {
			LOGGER.warn("Malformed URI", e);
		}

		return null;
	}

	@SubscribeEvent
	public static void tick(ServerTickEvent.Post event) {
		final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server != null) {
			get().tick(server);
		}
	}

	private void tick(MinecraftServer server) {
		proxy.connectWith(connectionConfig());
		proxy.tick();

		GameInstanceIntegrations instance = liveInstance;
		if (instance != null) {
			instance.tick(server);
		}
	}

	public GameInstanceIntegrations getOrOpen(GameStateMap instanceState, IGamePhase game) {
		GameInstanceIntegrations instance = instanceState.getOrRegister(GameInstanceIntegrations.KEY, new GameInstanceIntegrations(game, this));
		liveInstance = instance;
		return instance;
	}

	// TODO: It would be nice to have a more robust system for sending with retries - for example, if we send but the minigame didn't exist.. we probably shouldn't resend it
	void postAndRetry(final String endpoint, final JsonElement body) {
		postAndRetry(endpoint, body, 0);
	}

	private void postAndRetry(final String endpoint, final JsonElement body, final int depth) {
		EXECUTOR.submit(() -> {
			if (!sender.post(endpoint, body)) {
				if (depth <= MAX_RETRIES) {
					EXECUTOR.schedule(() -> {
						postAndRetry(endpoint, body, depth + 1);
					}, RETRY_DELAY_SECONDS, TimeUnit.SECONDS);
				}
			}
		});
	}

	void post(final String endpoint, final JsonElement body) {
		EXECUTOR.submit(() -> sender.post(endpoint, body));
	}

	void post(final String endpoint, final String body) {
		EXECUTOR.submit(() -> sender.post(endpoint, body));
	}

	void postPolling(final String endpoint, final JsonElement body) {
		EXECUTOR.submit(() -> pollSender.post(endpoint, body));
	}

	private void handlePayload(JsonObject object) {
		LOGGER.debug("Receive payload over websocket: {}", object);

		try {
			final String type = object.get("type").getAsString();
			final Crud crud = Crud.parse(object.get("crud"));
			if (crud == null) {
				LOGGER.error("Encountered unrecognized crud: '{}'", object.get("crud"));
				return;
			}

			handlePayload(object.getAsJsonObject("payload"), type, crud);
		} catch (Exception e) {
			LOGGER.error("An unexpected error occurred while trying to handle payload: {}", object, e);
		}
	}

	private void handlePayload(JsonObject object, String type, Crud crud) {
		GameInstanceIntegrations liveInstance = this.liveInstance;

		// we can ignore the payload because we will request it again when a minigame starts
		if (liveInstance == null) {
			return;
		}

		liveInstance.handlePayload(object, type, crud);
	}

	public boolean isConnected() {
		return DEBUG_LOGGING_BACKEND || proxy.isConnected();
	}

	void closeInstance(GameInstanceIntegrations instance) {
		if (liveInstance == instance) {
			liveInstance = null;
		}
	}

	public void sendOpen() {
		post(ConfigLT.INTEGRATIONS.worldLoadEndpoint.get(), "");
	}

	public void sendClose() {
		post(ConfigLT.INTEGRATIONS.worldUnloadEndpoint.get(), "");
	}
}
