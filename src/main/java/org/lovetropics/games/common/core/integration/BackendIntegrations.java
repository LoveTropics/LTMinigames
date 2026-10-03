package org.lovetropics.games.common.core.integration;

import com.google.common.base.Suppliers;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lovetropics.lib.techstack.Crud;
import com.lovetropics.lib.techstack.TechstackEventSubscriber;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Unit;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.config.ConfigLT;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.state.GameStateMap;
import org.lovetropics.games.common.core.integration.game_actions.GameActionType;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

@EventBusSubscriber(modid = LoveTropics.ID)
public final class BackendIntegrations {
	public static final boolean DEBUG_LOGGING_BACKEND = false;

	private static final Supplier<BackendIntegrations> INSTANCE = Suppliers.memoize(BackendIntegrations::new);

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final int RETRY_DELAY_SECONDS = 10;
	private static final int MAX_RETRIES = 6;

	private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(
			new ThreadFactoryBuilder()
					.setNameFormat("lt-integrations")
					.setDaemon(true)
					.build()
	);

	private final IntegrationSender sender = DEBUG_LOGGING_BACKEND ? IntegrationSender.LOGGING : IntegrationSender.open(ConfigLT.INTEGRATIONS.enabled, ConfigLT.INTEGRATIONS.authToken);

	private @Nullable TechstackEventSubscriber subscriber;
	private @Nullable GameInstanceIntegrations liveInstance;

	private String uri = "";
	private String token = "";

	private CompletableFuture<?> postFuture = CompletableFuture.completedFuture(null);

	private BackendIntegrations() {
	}

	public static BackendIntegrations get() {
		return INSTANCE.get();
	}

	private @Nullable TechstackEventSubscriber buildSubscriber(String uriString, String token) {
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
		this.uri = uri;
		this.token = token;
		if (subscriber != null) {
			subscriber.close();
			subscriber = buildSubscriber(uri, token);
		}
	}

	public void clearConfig() {
		uri = "";
		token = "";
		if (subscriber != null) {
			subscriber.close();
			subscriber = null;
		}
	}

	@SubscribeEvent
	public static void tick(ServerTickEvent.Post event) {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
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

	public void open(GameStateMap instanceState, IGamePhase game, Identifier backendId, String statisticsKey) {
		liveInstance = instanceState.getOrRegister(GameInstanceIntegrations.KEY, new GameInstanceIntegrations(game, backendId, statisticsKey, this));
	}

	private void schedulePost(Function<ScheduledExecutorService, CompletableFuture<?>> futureSupplier) {
		postFuture = postFuture.thenRunAsync(
				() -> futureSupplier.apply(EXECUTOR).exceptionally(throwable -> {
					LOGGER.error("Encountered exception in backend integrations sender", throwable);
					return null;
				}),
				EXECUTOR
		);
	}

	<T> void postAndRetry(String url, Codec<T> codec, T body) {
		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			LOGGER.warn("Cannot POST to URI because it is malformed: {}", url, e);
			return;
		}
		schedulePost(executor -> {
			CompletableFuture<?> future = new CompletableFuture<>();
			postAndRetryInner(future, executor, uri, codec, body, 0);
			return future;
		});
	}

	private <T> void postAndRetryInner(CompletableFuture<?> future, ScheduledExecutorService executor, URI uri, Codec<T> codec, T body, int depth) {
		IntegrationSender.PostResult result = sender.post(uri, codec, body);
		if (!result.shouldRetry() || depth > MAX_RETRIES) {
			future.complete(null);
		} else {
			executor.schedule(
					() -> postAndRetryInner(future, executor, uri, codec, body, depth + 1),
					RETRY_DELAY_SECONDS,
					TimeUnit.SECONDS
			);
		}
	}

	<T> void post(String url, Codec<T> codec, T body) {
		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			LOGGER.warn("Cannot POST to URI because it is malformed: {}", url, e);
			return;
		}
		schedulePost(_ -> {
			sender.post(uri, codec, body);
			return CompletableFuture.completedFuture(null);
		});
	}

	<T> CompletableFuture<Optional<T>> get(String url, Codec<T> codec) {
		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			LOGGER.warn("Cannot GET from URI because it is malformed: {}", url, e);
			return CompletableFuture.completedFuture(Optional.empty());
		}
		return CompletableFuture.supplyAsync(() -> sender.get(uri, codec), EXECUTOR);
	}

	public boolean isConnected() {
		return DEBUG_LOGGING_BACKEND || (subscriber != null && subscriber.isConnected());
	}

	void closeInstance(GameInstanceIntegrations instance) {
		if (liveInstance == instance) {
			liveInstance = null;
		}
	}

	@SubscribeEvent
	public static void onServerAboutToStart(ServerAboutToStartEvent event) {
		get().onServerAboutToStart();
	}

	@SubscribeEvent
	public static void onServerStopping(ServerStoppingEvent event) {
		get().onServerStop();
	}

	private void onServerAboutToStart() {
		post(ConfigLT.INTEGRATIONS.minigamesServiceUrl.get() + "/worldloaded", Unit.CODEC, Unit.INSTANCE);
		if (subscriber == null) {
			subscriber = buildSubscriber(uri, token);
		}
	}

	private void onServerStop() {
		post(ConfigLT.INTEGRATIONS.minigamesServiceUrl.get() + "/worldunloaded", Unit.CODEC, Unit.INSTANCE);
		if (subscriber != null) {
			subscriber.close();
			subscriber = null;
		}
	}
}
