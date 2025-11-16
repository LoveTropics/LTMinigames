package com.lovetropics.minigames.common.core.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lovetropics.lib.techstack.Crud;
import com.lovetropics.minigames.common.config.ConfigLT;
import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventListeners;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePackageEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GameTeamEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.SubGameEvents;
import com.lovetropics.minigames.common.core.game.behavior.instances.donation.DonationPackageData;
import com.lovetropics.minigames.common.core.game.state.GamePackageState;
import com.lovetropics.minigames.common.core.game.state.GameStateKey;
import com.lovetropics.minigames.common.core.game.state.IGameState;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.lovetropics.minigames.common.core.integration.game_actions.GameActionHandler;
import com.lovetropics.minigames.common.core.integration.game_actions.GameActionRequest;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class GameInstanceIntegrations implements IGameState {
	public static final GameStateKey<GameInstanceIntegrations> KEY = GameStateKey.create("Game Integrations");

	private static final Codec<List<DonationPackageData>> PACKAGES_CODEC = DonationPackageData.Payload.CODEC.codec()
			.xmap(DonationPackageData.Payload::data, DonationPackageData::asPayload)
			.listOf();

	private final UUID gameUuid = UUID.randomUUID();

	private final IGamePhase topLevelGame;
	private final Deque<IGamePhase> gameStack = new ArrayDeque<>();

	private final BackendIntegrations integrations;

	private final GameEventListeners phaseListeners = new GameEventListeners();

	private final GameActionHandler actions;

	private boolean closed;

	public GameInstanceIntegrations(IGamePhase topLevelGame, BackendIntegrations integrations) {
		this.topLevelGame = topLevelGame;
		this.integrations = integrations;
		actions = new GameActionHandler(this);

		gameStack.addLast(topLevelGame);

		phaseListeners.listen(GamePlayerEvents.REMOVE, p -> sendParticipantsList());
		phaseListeners.listen(GamePlayerEvents.SET_ROLE, (p, r, lr) -> sendParticipantsList());
		phaseListeners.listen(GameTeamEvents.TEAMS_ALLOCATED, this::sendParticipantsList);

		phaseListeners.listen(SubGameEvents.CREATE, (subGame, subEvents) -> {
			gameStack.addLast(subGame);
			subEvents.listen(GamePhaseEvents.DESTROY, () -> {
				gameStack.removeLast();
				sendPackagesUpdate();
				sendParticipantsList();
			});
			sendPackagesUpdate();
			sendParticipantsList();
		});
	}

	public void start(IGamePhase phase, EventRegistrar events, @Nullable PlayerKey initiator) {
		if (phase != gameStack.peekLast()) {
			throw new IllegalStateException("Tried to send start event for game that was not active");
		}

		if (phase == topLevelGame) {
			sendMinigameStart(initiator);
			requestQueuedActions();
		}

		events.addAll(phaseListeners);
	}

	private void sendMinigameStart(@Nullable PlayerKey initiator) {
		JsonObject payload = new JsonObject();
		if (initiator != null) {
			payload.add("initiator", initiator.serializeProfile());
		}
		payload.add("participants", serializeParticipantsArray());
		payload.add("teams", serializeTeamsArray());
		addGameDefinitionData(payload);

		postImportant(ConfigLT.INTEGRATIONS.minigameStartEndpoint.get(), payload);
	}

	private void sendPackagesUpdate() {
		JsonObject payload = new JsonObject();
		addGameDefinitionData(payload);
		postImportant(ConfigLT.INTEGRATIONS.minigameUpdatePackagesEndpoint.get(), payload);
	}

	private void addGameDefinitionData(JsonObject payload) {
		IGameDefinition definition = topLevelGame.definition();
		payload.add("name", ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, definition.name()).getOrThrow());
		Component subtitle = definition.subtitle();
		if (subtitle != null) {
			payload.add("subtitle", ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, subtitle).getOrThrow());
		}

		IGamePhase activeGame = activeGame();
		GamePackageState packageState = activeGame.state().getOrNull(GamePackageState.KEY);
		List<DonationPackageData> packageList = packageState != null ? List.copyOf(packageState.packages()) : List.of();
		payload.add("packages", PACKAGES_CODEC.encodeStart(JsonOps.INSTANCE, packageList).getOrThrow());
	}

	public void finish(IGamePhase phase) {
		if (phase == topLevelGame) {
			JsonObject payload = new JsonObject();
			payload.addProperty("finish_time_utc", Instant.now().getEpochSecond());
			payload.add("statistics", phase.statistics().serialize());
			payload.add("participants", serializeParticipantsArray());
			payload.add("teams", serializeTeamsArray());

			postImportant(ConfigLT.INTEGRATIONS.minigameEndEndpoint.get(), payload);

			close();
		}
	}

	public void cancel(IGamePhase phase) {
		if (phase == topLevelGame) {
			postImportant(ConfigLT.INTEGRATIONS.minigameCancelEndpoint.get(), new JsonObject());
			close();
		}
	}

	public void acknowledgeActionDelivery(final GameActionRequest request) {
		final JsonObject object = new JsonObject();
		object.addProperty("request", request.type().getId());
		object.addProperty("uuid", request.uuid().toString());

		integrations.postAndRetry(ConfigLT.INTEGRATIONS.actionResolvedEndpoint.get(), object);
	}

	public void createPoll(String title, String duration, String... options) {
		if (options.length < 2) {
			throw new IllegalArgumentException("Poll must have more than 1 choice");
		}
		JsonObject object = new JsonObject();
		object.addProperty("title", title);
		object.addProperty("start", Instant.now().getEpochSecond());
		object.addProperty("duration", duration);
		JsonArray array = new JsonArray();
		for (String option : options) {
			array.add(option);
		}
		object.add("options", array);
		integrations.postPolling(ConfigLT.INTEGRATIONS.addPollEndpoint.get(), object);
	}

	private void sendParticipantsList() {
		JsonObject payload = new JsonObject();
		payload.add("participants", serializeParticipantsArray());
		payload.add("teams", serializeTeamsArray());
		post(ConfigLT.INTEGRATIONS.minigamePlayerUpdateEndpoint.get(), payload);
	}

	private JsonArray serializeParticipantsArray() {
		JsonArray participantsArray = new JsonArray();
		for (ServerPlayer participant : activeGame().participants()) {
			participantsArray.add(PlayerKey.from(participant).serializeProfile());
		}
		return participantsArray;
	}

	private IGamePhase activeGame() {
		return gameStack.getLast();
	}

	private JsonArray serializeTeamsArray() {
		TeamState teams = activeGame().instanceState().getOrNull(TeamState.KEY);
		if (teams == null) {
			return new JsonArray();
		}
		JsonArray teamsArray = new JsonArray();
		for (GameTeam team : teams) {
			teamsArray.add(GameTeam.Payload.CODEC.encodeStart(JsonOps.INSTANCE, team.asPayload()).getOrThrow());
		}
		return teamsArray;
	}

	private void requestQueuedActions() {
		post(ConfigLT.INTEGRATIONS.pendingActionsEndpoint.get(), new JsonObject());
	}

	private void post(String endpoint, JsonObject payload) {
		post(endpoint, payload, false);
	}

	private void postImportant(String endpoint, JsonObject payload) {
		post(endpoint, payload, true);
	}

	private void post(String endpoint, JsonObject payload, boolean important) {
		if (closed) {
			return;
		}

		payload.addProperty("id", gameUuid.toString());

		IGameDefinition definition = topLevelGame.definition();
		JsonObject game = new JsonObject();
		game.addProperty("id", definition.backendId().toString());
		game.addProperty("telemetry_key", definition.statisticsKey());
		game.addProperty("name", definition.name().getString());
		payload.add("minigame", game);

		if (important) {
			integrations.postAndRetry(endpoint, payload);
		} else {
			integrations.post(endpoint, payload);
		}
	}

	public <T> CompletableFuture<Optional<T>> get(String endpoint, Codec<T> codec) {
		return integrations.get(endpoint, codec);
	}

	private void close() {
		closed = true;
		integrations.closeInstance(this);
	}

	void tick(MinecraftServer server) {
		if (!closed) {
			actions.pollGameActions(activeGame(), server.getTickCount());
		}
	}

	void handleActionRequest(GameActionRequest actionRequest) {
		if (!closed) {
			actions.enqueue(actionRequest);
		}
	}

	void handlePoll(JsonObject object, Crud crud) {
		if (!closed) {
			activeGame().invoker(GamePackageEvents.RECEIVE_POLL_EVENT).onReceivePollEvent(object, crud);
		}
	}
}
