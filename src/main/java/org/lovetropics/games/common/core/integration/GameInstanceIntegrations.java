package org.lovetropics.games.common.core.integration;

import com.google.gson.JsonObject;
import com.lovetropics.lib.techstack.Crud;
import com.lovetropics.minigames.common.core.game.GameDonationType;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class GameInstanceIntegrations implements IGameState {
	public static final GameStateKey<GameInstanceIntegrations> KEY = GameStateKey.create("Game Integrations");

	private static final Codec<List<DonationPackageData>> PACKAGES_CODEC = DonationPackageData.Payload.CODEC.codec()
			.xmap(DonationPackageData.Payload::data, DonationPackageData::asPayload)
			.listOf();

	private static final GameEventType<StartGame> EVENT_START_GAME = GameEventType.createImportant("/start", StartGame.MAP_CODEC);
	private static final GameEventType<PlayersAndTeams> EVENT_UPDATE_PLAYERS = GameEventType.create("/playerupdate", PlayersAndTeams.MAP_CODEC);
	private static final GameEventType<Unit> EVENT_REQUEST_PENDING_ACTIONS = GameEventType.create("/pendingactions", MapCodec.unit(Unit.INSTANCE));
	private static final GameEventType<UpdatePackages> EVENT_UPDATE_PACKAGES = GameEventType.createImportant("/updatepackages", UpdatePackages.MAP_CODEC);
	private static final GameEventType<FinishGame> EVENT_FINISH_GAME = GameEventType.createImportant("/end", FinishGame.MAP_CODEC);
	private static final GameEventType<Unit> EVENT_CANCEL_GAME = GameEventType.createImportant("/cancel", MapCodec.unit(Unit.INSTANCE));

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
		events.listen(GamePlayerEvents.REMOVE, _ -> post(EVENT_UPDATE_PLAYERS, packPlayersAndTeams()));
		events.listen(GamePlayerEvents.SET_ROLE, (_, _, _) -> post(EVENT_UPDATE_PLAYERS, packPlayersAndTeams()));
		events.listen(GameTeamEvents.TEAMS_ALLOCATED, _ -> post(EVENT_UPDATE_PLAYERS, packPlayersAndTeams()));

		addSubGameListeners(events);
	}

	private void addSubGameListeners(EventRegistrar events) {
		events.listen(SubGameEvents.CREATE, (subGame, subEvents) -> {
			allGames.add(subGame);
			subEvents.listen(GamePhaseEvents.DESTROY, () -> {
				allGames.remove(subGame);
				post(EVENT_UPDATE_PACKAGES, new UpdatePackages(collectPackages()));
				post(EVENT_UPDATE_PLAYERS, packPlayersAndTeams());
			});
			post(EVENT_UPDATE_PACKAGES, new UpdatePackages(collectPackages()));
			post(EVENT_UPDATE_PLAYERS, packPlayersAndTeams());
			addSubGameListeners(subEvents);
		});
	}

	public void start(IGamePhase phase, EventRegistrar events, @Nullable PlayerKey initiator) {
		if (phase != topLevelGame) {
			return;
		}

		IGameDefinition definition = topLevelGame.definition();
		post(EVENT_START_GAME, new StartGame(
				definition.name(),
				Optional.ofNullable(definition.subtitle()),
				Optional.ofNullable(initiator).map(PlayerKey::nameAndId),
				packPlayersAndTeams(),
				collectPackages(),
				definition.donationType()
		));
		post(EVENT_REQUEST_PENDING_ACTIONS, Unit.INSTANCE);

		addListeners(events);
	}

	private List<DonationPackageData> collectPackages() {
		Set<DonationPackageData> allPackages = new ObjectOpenHashSet<>();
		for (IGamePhase game : allGames) {
			GamePackageState packageState = game.state().getOrNull(GamePackageState.KEY);
			if (packageState != null) {
				allPackages.addAll(packageState.packages());
			}
		}

		return allPackages.stream()
				.sorted(Comparator.comparing(DonationPackageData::id))
				.toList();
	}

	public void finish(IGamePhase phase) {
		if (phase == topLevelGame) {
			post(EVENT_FINISH_GAME, new FinishGame(
					Instant.now(),
					phase.statistics(),
					packPlayersAndTeams()
			));
			close();
		}
	}

	public void cancel(IGamePhase phase) {
		if (phase == topLevelGame) {
			post(EVENT_CANCEL_GAME, Unit.INSTANCE);
			close();
		}
	}

	public void acknowledgeActionDelivery(GameActionRequest request) {
		integrations.postAndRetry(ConfigLT.INTEGRATIONS.minigamesServiceUrl.get() + "/actionresolved", ActionAcknowledgement.CODEC, new ActionAcknowledgement(
				request.type(),
				request.uuid()
		));
	}

	public void createPoll(String title, String duration, String... options) {
		if (options.length < 2) {
			throw new IllegalArgumentException("Poll must have more than 1 choice");
		}
		integrations.post(ConfigLT.INTEGRATIONS.pollsServiceUrl.get() + "/add", CreatePoll.MAP_CODEC.codec(), new CreatePoll(
				title,
				Instant.now(),
				duration,
				List.of(options)
		));
	}

	private PlayersAndTeams packPlayersAndTeams() {
		TeamState teams = topLevelGame.instanceState().getOrNull(TeamState.KEY);

		List<Participant> participants = allGames.stream()
				.flatMap(game -> game.participants().stream())
				.map(this::packParticipant)
				.toList();

		return new PlayersAndTeams(
				participants,
				teams != null ? teams.stream().map(GameTeam::asPayload).toList() : List.of()
		);
	}

	private Participant packParticipant(ServerPlayer player) {
		MinecraftSessionService sessionService = player.level().getServer().services().sessionService();
		GameProfile profile = player.getGameProfile();
		Optional<Participant.Skin> skin = Optional.ofNullable(sessionService.getTextures(profile).skin()).map(skinTexture -> new Participant.Skin(
				skinTexture.getUrl(),
				Objects.requireNonNullElse(skinTexture.getMetadata("model"), "default")
		));
		return new Participant(profile.id(), profile.name(), skin);
	}

	private <T> void post(GameEventType<T> type, T payload) {
		if (closed) {
			return;
		}

		GameEvent<T> event = new GameEvent<>(gameUuid, gameTypeDefinition, payload);
		String uri = ConfigLT.INTEGRATIONS.minigamesServiceUrl.get() + type.path;
		if (type.important) {
			integrations.postAndRetry(uri, type.eventCodec, event);
		} else {
			integrations.post(uri, type.eventCodec, event);
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
			Optional<NameAndId> initiator,
			PlayersAndTeams playersAndTeams,
			List<DonationPackageData> packages,
			GameDonationType donationType
	) {
		public static final MapCodec<StartGame> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				ComponentSerialization.CODEC.fieldOf("name").forGetter(StartGame::name),
				ComponentSerialization.CODEC.optionalFieldOf("subtitle").forGetter(StartGame::subtitle),
				NameAndId.CODEC.optionalFieldOf("initiator").forGetter(StartGame::initiator),
				PlayersAndTeams.MAP_CODEC.forGetter(StartGame::playersAndTeams),
				PACKAGES_CODEC.fieldOf("packages").forGetter(StartGame::packages),
				GameDonationType.CODEC.optionalFieldOf("donation_type", GameDonationType.PACKAGES).forGetter(StartGame::donationType)
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
			List<DonationPackageData> packages
	) {
		public static final MapCodec<UpdatePackages> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
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
			UUID id,
			String name,
			Optional<Skin> skin
	) {
		public static final Codec<Participant> CODEC = RecordCodecBuilder.create(i -> i.group(
				UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(Participant::id),
				Codec.STRING.fieldOf("name").forGetter(Participant::name),
				Skin.CODEC.optionalFieldOf("skin").forGetter(Participant::skin)
		).apply(i, Participant::new));

		public record Skin(
				String url,
				String model
		) {
			public static final Codec<Skin> CODEC = RecordCodecBuilder.create(i -> i.group(
					Codec.STRING.fieldOf("url").forGetter(Skin::url),
					Codec.STRING.fieldOf("model").forGetter(Skin::model)
			).apply(i, Skin::new));
		}
	}

	private record GameEventType<T>(
			String path,
			MapCodec<T> payload,
			boolean important,
			Codec<GameEvent<T>> eventCodec
	) {
		public static <T> GameEventType<T> create(String path, MapCodec<T> payload) {
			return new GameEventType<>(path, payload, false, GameEvent.codec(payload));
		}

		public static <T> GameEventType<T> createImportant(String path, MapCodec<T> payload) {
			return new GameEventType<>(path, payload, true, GameEvent.codec(payload));
		}
	}
}
