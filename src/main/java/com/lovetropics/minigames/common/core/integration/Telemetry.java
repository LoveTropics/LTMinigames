package com.lovetropics.minigames.common.core.integration;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lovetropics.lib.backend.BackendConnection;
import com.lovetropics.lib.backend.BackendProxy;
import com.lovetropics.minigames.Constants;
import com.lovetropics.minigames.common.config.ConfigLT;
import com.lovetropics.minigames.common.core.game.IGameInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Constants.MODID)
public final class Telemetry {
	public static final Telemetry INSTANCE = new Telemetry();

	private static final Logger LOGGER = LogManager.getLogger(Telemetry.class);

	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(
			new ThreadFactoryBuilder()
					.setNameFormat("lt-telemetry")
					.setDaemon(true)
					.build()
	);

	private final TelemetrySender sender = TelemetrySender.Http.openFromConfig();
	private final TelemetrySender pollSender = new TelemetrySender.Http(() -> "https://polling.lovetropics.com", ConfigLT.TELEMETRY.authToken::get);

	private final BackendProxy proxy;

	private GameInstanceTelemetry instance;

	private Telemetry() {
		Supplier<URI> address = () -> {
			if (Strings.isNullOrEmpty(ConfigLT.TELEMETRY.authToken.get())) {
				return null;
			}

			try {
				int configPort = ConfigLT.TELEMETRY.webSocketPort.get();
				String port = configPort == 0 ? ":443" : ":" + configPort;
				return new URI("wss://" + ConfigLT.TELEMETRY.webSocketUrl.get() + port + "/ws");
			} catch (URISyntaxException e) {
				LOGGER.warn("Malformed URI", e);
				return null;
			}
		};

		this.proxy = new BackendProxy(address, new BackendConnection.Handler() {
			@Override
			public void acceptOpened() {
			}

			@Override
			public void acceptMessage(JsonObject payload) {
				handlePayload(payload);
			}

			@Override
			public void acceptError(Throwable cause) {
				LOGGER.error("Telemetry websocket closed with error", cause);
			}

			@Override
			public void acceptClosed(int code, @Nullable String reason) {
				LOGGER.error("Telemetry websocket closed with code: {} and reason: {}", code, reason);
			}
		});
	}

	@SubscribeEvent
	public static void tick(TickEvent.ServerTickEvent event) {
		final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) {
			return;
		}

		if (event.phase == TickEvent.Phase.END) {
			Telemetry.INSTANCE.tick(server);
		}
	}

	private void tick(MinecraftServer server) {
		proxy.tick();

		if (instance != null) {
			instance.tick(server);
		}
	}

	public GameInstanceTelemetry openGame(IGameInstance minigame) {
		return GameInstanceTelemetry.open(minigame, this);
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

	CompletableFuture<JsonElement> get(final String endpoint) {
		return CompletableFuture.supplyAsync(() -> sender.get(endpoint), EXECUTOR);
	}

	private void handlePayload(JsonObject object) {
		LOGGER.debug("Receive payload over websocket: {}", object);

		try {
			final String type = object.get("type").getAsString();
			final String crud = object.get("crud").getAsString();

			handlePayload(object.getAsJsonObject("payload"), type, crud);
		} catch (Exception e) {
			LOGGER.error("An unexpected error occurred while trying to handle payload: {}", object, e);
		}
	}

	private void handlePayload(JsonObject object, String type, String crud) {
		GameInstanceTelemetry instance = this.instance;

		// we can ignore the payload because we will request it again when a minigame starts
		if (instance == null) return;

		instance.handlePayload(object, type, crud);
	}

	public boolean isConnected() {
		return proxy.isConnected();
	}

	void openInstance(GameInstanceTelemetry instance) {
		this.instance = instance;
	}

	void closeInstance(GameInstanceTelemetry instance) {
		if (this.instance == instance) {
			this.instance = null;
		}
	}

	public void sendOpen() {
		post(ConfigLT.TELEMETRY.worldLoadEndpoint.get(), "");
	}

	public void sendClose() {
		post(ConfigLT.TELEMETRY.worldUnloadEndpoint.get(), "");
	}
}