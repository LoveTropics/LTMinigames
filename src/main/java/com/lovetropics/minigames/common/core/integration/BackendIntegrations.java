package com.lovetropics.minigames.common.core.integration;

import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonElement;
import com.lovetropics.lib.techstack.Crud;
import com.lovetropics.lib.techstack.TechstackEventSubscriber;
import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.config.ConfigLT;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.state.GameStateMap;
import com.lovetropics.minigames.common.core.integration.game_actions.GameActionType;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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

	@Nullable
	private TechstackEventSubscriber subscriber;
	@Nullable
	private GameInstanceIntegrations liveInstance;

	private BackendIntegrations() {
	}

	public static BackendIntegrations get() {
		return INSTANCE.get();
	}

	@Nullable
	private TechstackEventSubscriber buildSubscriber(String uriString, String token) {
		if (uriString.isBlank() || token.isBlank()) {
			return null;
		}

		URI uri;
		try {
			uri = new URI(uriString);
		} catch (URISyntaxException e) {
			LOGGER.warn("Malformed URI", e);
			return null;
		}

		TechstackEventSubscriber.Builder subscriber = TechstackEventSubscriber.builder(uri)
				.authenticate(token);

		addEventSubscriptions(subscriber);

		return subscriber.build();
	}

	private void addEventSubscriptions(TechstackEventSubscriber.Builder subscriber) {
		for (GameActionType type : GameActionType.values()) {
			subscriber.subscribe(Crud.CREATE, type.getId(), type.codec(), action -> {
				if (liveInstance != null) {
					liveInstance.handleActionRequest(action);
				}
			});
		}

		// TODO: Do something better than this
		for (Crud crud : Crud.values()) {
			subscriber.subscribe(crud, "poll", ExtraCodecs.JSON, payload -> {
				if (liveInstance != null) {
					liveInstance.handlePoll(payload.getAsJsonObject(), crud);
				}
			});
		}
	}

	public void updateConfig(String uri, String token) {
		if (subscriber != null) {
			subscriber.close();
		}
		subscriber = buildSubscriber(uri, token);
	}

	@SubscribeEvent
	public static void tick(ServerTickEvent.Post event) {
		final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server != null) {
			get().tick(server);
		}
	}

	private void tick(MinecraftServer server) {
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

	<T> CompletableFuture<Optional<T>> get(final String endpoint, final Codec<T> codec) {
		return CompletableFuture.supplyAsync(() -> sender.get(endpoint, codec), EXECUTOR);
	}

	public boolean isConnected() {
		return DEBUG_LOGGING_BACKEND || (subscriber != null && subscriber.isConnected());
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
		if (subscriber != null) {
			subscriber.close();
			subscriber = null;
		}
	}
}
