package org.lovetropics.games.common.core.integration;

import com.google.gson.JsonObject;
import com.lovetropics.lib.techstack.Crud;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.config.ConfigLT;
import org.lovetropics.games.common.core.game.IGameDefinition;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePackageEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.behavior.event.GameTeamEvents;
import org.lovetropics.games.common.core.game.behavior.event.SubGameEvents;
import org.lovetropics.games.common.core.game.behavior.instances.donation.DonationPackageData;
import org.lovetropics.games.common.core.game.state.GamePackageState;
import org.lovetropics.games.common.core.game.state.GameStateKey;
import org.lovetropics.games.common.core.game.state.IGameState;
import org.lovetropics.games.common.core.game.state.statistics.GameStatistics;
import org.lovetropics.games.common.core.game.state.statistics.PlayerKey;
import org.lovetropics.games.common.core.game.state.team.GameTeam;
import org.lovetropics.games.common.core.game.state.team.TeamState;
import org.lovetropics.games.common.core.integration.game_actions.GameActionHandler;
import org.lovetropics.games.common.core.integration.game_actions.GameActionRequest;
import org.lovetropics.games.common.core.integration.game_actions.GameActionType;
import org.lovetropics.games.common.util.Codecs;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class GameInstanceIntegrations implements IGameState {
	public static final GameStateKey<GameInstanceIntegrations> KEY = GameStateKey.create("Game Integrations");

	private static final Codec<List<DonationPackageData>> PACKAGES_CODEC = DonationPackageData.Payload.CODEC.codec()
			.xmap(DonationPackageData.Payload::data, DonationPackageData::asPayload)
			.listOf();

	private final UUID gameUuid = UUID.randomUUID();

	private final IGamePhase topLevelGame;
	private final GameTypeDefinition gameTypeDefinition;
	private final List<IGamePhase> allGames = new ArrayList<>();

	private final BackendIntegrations integrations;

	private final GameActionHandler actions;

	private boolean closed;

	public GameInstanceIntegrations(IGamePhase topLevelGame, Identifier backendId, String statisticsKey, BackendIntegrations integrations) {
		this.topLevelGame = topLevelGame;
		gameTypeDefinition = new GameTypeDefinition(backendId, statisticsKey, topLevelGame.definition().name().getString());
		this.integrations = integrations;
		actions = new GameActionHandler(this);

		allGames.addLast(topLevelGame);
	}

	private void addListeners(EventRegistrar events) {
		events.listen(GamePlayerEvents.REMOVE, p -> sendParticipantsList());
		events.listen(GamePlayerEvents.SET_ROLE, (p, r, lr) -> sendParticipantsList());
		events.listen(GameTeamEvents.TEAMS_ALLOCATED, p -> sendParticipantsList());

		addSubGameListeners(events);
	}

	private void addSubGameListeners(EventRegistrar events) {
		events.listen(SubGameEvents.CREATE, (subGame, subEvents) -> {
			allGames.add(subGame);
			subEvents.listen(GamePhaseEvents.DESTROY, () -> {
				allGames.remove(subGame);
				sendPackagesUpdate();
				sendParticipantsList();
			});
			sendPackagesUpdate();
			sendParticipantsList();
			addSubGameListeners(subEvents);
		});
	}

	public void start(IGamePhase phase, EventRegistrar events, @Nullable PlayerKey initiator) {
		if (phase != topLevelGame) {
			return;
		}

		sendMinigameStart(initiator);
		requestQueuedActions();

		addListeners(events);
	}

	private void sendMinigameStart(@Nullable PlayerKey initiator) {
		IGameDefinition definition = topLevelGame.definition();
		postImportant(ConfigLT.INTEGRATIONS.minigameStartEndpoint.get(), StartGame.MAP_CODEC, new StartGame(
				definition.name(),
				Optional.ofNullable(definition.subtitle()),
				Optional.ofNullable(initiator).map(Participant::new),
				packPlayersAndTeams()
		));
	}

	private void sendPackagesUpdate() {
		IGameDefinition definition = topLevelGame.definition();

		Set<DonationPackageData> allPackages = new ObjectOpenHashSet<>();
		for (IGamePhase game : allGames) {
			GamePackageState packageState = game.state().getOrNull(GamePackageState.KEY);
			if (packageState != null) {
				allPackages.addAll(packageState.packages());
			}
		}

		List<DonationPackageData> sortedPackages = allPackages.stream()
				.sorted(Comparator.comparing(DonationPackageData::id))
				.toList();

		postImportant(ConfigLT.INTEGRATIONS.minigameUpdatePackagesEndpoint.get(), UpdatePackages.MAP_CODEC, new UpdatePackages(
				definition.name(),
				Optional.ofNullable(definition.subtitle()),
				sortedPackages
		));
	}

	public void finish(IGamePhase phase) {
		if (phase == topLevelGame) {
			postImportant(ConfigLT.INTEGRATIONS.minigameEndEndpoint.get(), FinishGame.MAP_CODEC, new FinishGame(
					Instant.now(),
					phase.statistics(),
					packPlayersAndTeams()
			));
			close();
		}
	}

	public void cancel(IGamePhase phase) {
		if (phase == topLevelGame) {
			postImportant(ConfigLT.INTEGRATIONS.minigameCancelEndpoint.get(), MapCodec.unit(Unit.INSTANCE), Unit.INSTANCE);
			close();
		}
	}

	public void acknowledgeActionDelivery(GameActionRequest request) {
		integrations.postAndRetry(ConfigLT.INTEGRATIONS.actionResolvedEndpoint.get(), ActionAcknowledgement.CODEC, new ActionAcknowledgement(
				request.type(),
				request.uuid()
		));
	}

	public void createPoll(String title, String duration, String... options) {
		if (options.length < 2) {
			throw new IllegalArgumentException("Poll must have more than 1 choice");
		}
		integrations.postPolling(ConfigLT.INTEGRATIONS.addPollEndpoint.get(), CreatePoll.MAP_CODEC.codec(), new CreatePoll(
				title,
				Instant.now(),
				duration,
				List.of(options)
		));
	}

	private void sendParticipantsList() {
		post(ConfigLT.INTEGRATIONS.minigamePlayerUpdateEndpoint.get(), PlayersAndTeams.MAP_CODEC, packPlayersAndTeams());
	}

	private PlayersAndTeams packPlayersAndTeams() {
		TeamState teams = topLevelGame.instanceState().getOrNull(TeamState.KEY);

		List<Participant> participants = allGames.stream()
				.flatMap(game -> game.participants().stream())
				.map(player -> new Participant(PlayerKey.from(player)))
				.toList();

		return new PlayersAndTeams(
				participants,
				teams != null ? teams.stream().map(GameTeam::asPayload).toList() : List.of()
		);
	}

	private void requestQueuedActions() {
		post(ConfigLT.INTEGRATIONS.pendingActionsEndpoint.get(), MapCodec.unit(Unit.INSTANCE), Unit.INSTANCE);
	}

	private <T> void post(String endpoint, MapCodec<T> codec, T payload) {
		post(endpoint, codec, payload, false);
	}

	private <T> void postImportant(String endpoint, MapCodec<T> codec, T payload) {
		post(endpoint, codec, payload, true);
	}

	private <T> void post(String endpoint, MapCodec<T> codec, T payload, boolean important) {
		if (closed) {
			return;
		}

		GameEvent<T> event = new GameEvent<>(gameUuid, gameTypeDefinition, payload);
		if (important) {
			integrations.postAndRetry(endpoint, GameEvent.codec(codec), event);
		} else {
			integrations.post(endpoint, GameEvent.codec(codec), event);
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
			actions.pollGameActions(allGames, server.getTickCount());
		}
	}

	void handleActionRequest(GameActionRequest actionRequest) {
		if (!closed) {
			actions.enqueue(actionRequest);
		}
	}

	void handlePoll(JsonObject object, Crud crud) {
		if (!closed) {
			for (IGamePhase game : allGames) {
				game.invoker(GamePackageEvents.RECEIVE_POLL_EVENT).onReceivePollEvent(object, crud);
			}
		}
	}

	private record GameEvent<T>(
			UUID id,
			GameTypeDefinition minigame,
			T payload
	) {
		public static <T> Codec<GameEvent<T>> codec(MapCodec<T> payloadCodec) {
			return RecordCodecBuilder.create(i -> i.group(
					UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(GameEvent::id),
					GameTypeDefinition.CODEC.fieldOf("minigame").forGetter(GameEvent::minigame),
					payloadCodec.forGetter(GameEvent::payload)
			).apply(i, GameEvent<T>::new));
		}
	}

	private record GameTypeDefinition(
			Identifier id,
			String telemetryKey,
			String name
	) {
		public static final Codec<GameTypeDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
				Identifier.CODEC.fieldOf("id").forGetter(GameTypeDefinition::id),
				Codec.STRING.fieldOf("telemetry_key").forGetter(GameTypeDefinition::telemetryKey),
				Codec.STRING.fieldOf("name").forGetter(GameTypeDefinition::name)
		).apply(i, GameTypeDefinition::new));
	}

	private record StartGame(
			Component name,
			Optional<Component> subtitle,
			Optional<Participant> initiator,
			PlayersAndTeams playersAndTeams
	) {
		public static final MapCodec<StartGame> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				ComponentSerialization.CODEC.fieldOf("name").forGetter(StartGame::name),
				ComponentSerialization.CODEC.optionalFieldOf("subtitle").forGetter(StartGame::subtitle),
				Participant.CODEC.optionalFieldOf("initiator").forGetter(StartGame::initiator),
				PlayersAndTeams.MAP_CODEC.forGetter(StartGame::playersAndTeams)
		).apply(i, StartGame::new));
	}

	private record FinishGame(
			Instant finishTime,
			GameStatistics statistics,
			PlayersAndTeams playersAndTeams
	) {
		public static final MapCodec<FinishGame> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codecs.EPOCH_SECOND.fieldOf("finish_time_utc").forGetter(FinishGame::finishTime),
				GameStatistics.CODEC.fieldOf("statistics").forGetter(FinishGame::statistics),
				PlayersAndTeams.MAP_CODEC.forGetter(FinishGame::playersAndTeams)
		).apply(i, FinishGame::new));
	}

	private record UpdatePackages(
			// TODO: Why are these here? We don't even use them in the backend
			Component name,
			Optional<Component> subtitle,
			List<DonationPackageData> packages
	) {
		public static final MapCodec<UpdatePackages> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				ComponentSerialization.CODEC.fieldOf("name").forGetter(UpdatePackages::name),
				ComponentSerialization.CODEC.optionalFieldOf("subtitle").forGetter(UpdatePackages::subtitle),
				PACKAGES_CODEC.fieldOf("packages").forGetter(UpdatePackages::packages)
		).apply(i, UpdatePackages::new));
	}

	private record ActionAcknowledgement(
			GameActionType request,
			UUID uuid
	) {
		public static final Codec<ActionAcknowledgement> CODEC = RecordCodecBuilder.create(i -> i.group(
				GameActionType.CODEC.fieldOf("request").forGetter(ActionAcknowledgement::request),
				UUIDUtil.STRING_CODEC.fieldOf("uuid").forGetter(ActionAcknowledgement::uuid)
		).apply(i, ActionAcknowledgement::new));
	}

	private record CreatePoll(
			String title,
			Instant start,
			String duration,
			List<String> options
	) {
		public static final MapCodec<CreatePoll> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Codec.STRING.fieldOf("title").forGetter(CreatePoll::title),
				Codecs.EPOCH_SECOND.fieldOf("start").forGetter(CreatePoll::start),
				Codec.STRING.fieldOf("duration").forGetter(CreatePoll::duration),
				Codec.STRING.listOf().fieldOf("options").forGetter(CreatePoll::options)
		).apply(i, CreatePoll::new));
	}

	private record PlayersAndTeams(
			List<Participant> participants,
			List<GameTeam.Payload> teams
	) {
		public static final MapCodec<PlayersAndTeams> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				Participant.CODEC.listOf().fieldOf("participants").forGetter(PlayersAndTeams::participants),
				GameTeam.Payload.CODEC.listOf().fieldOf("teams").forGetter(PlayersAndTeams::teams)
		).apply(i, PlayersAndTeams::new));
	}

	private record Participant(
			PlayerKey key
	) {
		public static final Codec<Participant> CODEC = PlayerKey.FULL_CODEC.xmap(Participant::new, Participant::key);
	}
}
