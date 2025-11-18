package com.lovetropics.minigames.common.content.escape_race.behaviours;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.content.escape_race.EscapeRaceTexts;
import com.lovetropics.minigames.common.content.escape_race.client.EscapeRaceRoomsState;
import com.lovetropics.minigames.common.content.escape_race.misc.RoomEntrancePadEntity;
import com.lovetropics.minigames.common.content.escape_race.rooms.RoomStatus;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.PendingSubPhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.command.GameCommandRegistrar;
import com.lovetropics.minigames.common.core.game.config.GameConfig;
import com.lovetropics.minigames.common.core.game.config.GameConfigs;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.lovetropics.minigames.common.core.game.util.GameBossBar;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record WarehouseSetupBehaviour(
		List<RoomConfig> rooms
) implements IGameBehavior {
	public static final MapCodec<WarehouseSetupBehaviour> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(RoomConfig.CODEC.listOf().fieldOf("rooms").forGetter(WarehouseSetupBehaviour::rooms)).apply(inst, WarehouseSetupBehaviour::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		State state = new State();
		TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);

		events.listen(GamePhaseEvents.CREATE, () -> this.onGameStarted(game, state));
		events.listen(GamePhaseEvents.TICK, () -> {
			for (RoomInstance room : state.rooms.values()) {
				tickRoom(game, teams, room);
			}
		});

		GameClientState.applyGlobally(game, events, SharedConstants.TICKS_PER_SECOND, EscapeRace.ROOMS_STATE.get(), player -> {
			GameTeamKey teamForPlayer = teams.getTeamForPlayer(player);
			return state.asClientState(teamForPlayer);
		});

		events.listen(GamePlayerEvents.JOIN, player -> {
			GameTeamKey team = teams.getTeamForPlayer(player);
			if (team == null) {
				return;
			}
			for (RoomInstance room : state.rooms.values()) {
				TeamRoomInstance teamRoom = room.getTeamRoom(team);
				if (teamRoom.maybeTransferPlayer(game, player)) {
					return;
				}
			}
		});

		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) ->
				registerCommands(commands, state)
		);
	}

	private void registerCommands(GameCommandRegistrar commands, State state) {
		commands.register(Commands.literal("room")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.argument("room", StringArgumentType.word())
						.suggests((context, builder) ->
								SharedSuggestionProvider.suggest(state.rooms.keySet().stream(), builder)
						)
						.then(Commands.literal("cost").then(Commands.literal("set")
								.then(Commands.argument("cost", IntegerArgumentType.integer(0))
										.executes(context -> {
											RoomInstance room = getRoomArgument(context, state);
											room.cost = IntegerArgumentType.getInteger(context, "cost");
											return 1;
										})
								)
						))
				)
		);
	}

	private static RoomInstance getRoomArgument(CommandContext<CommandSourceStack> context, State state) throws CommandSyntaxException {
		String roomId = StringArgumentType.getString(context, "room");
		RoomInstance room = state.rooms.get(roomId);
		if (room == null) {
			throw new SimpleCommandExceptionType(Component.literal("No room with id: " + roomId)).create();
		}
		return room;
	}

	private void tickRoom(IGamePhase game, TeamState teams, RoomInstance room) {
		if (room.unlockingState != null) {
			if (tickUnlocking(game, teams, room, room.unlockingState)) {
				room.unlockingState = null;
			}
		} else {
			room.entrancePad.setUnlockingTicks(0, false);
		}

		for (GameTeamKey team : teams.getTeamKeys()) {
			TeamRoomInstance teamState = room.getTeamRoom(team);
			if (teamState.status != RoomStatus.LOCKED) {
				teamState.unlockingBar.setPlayers(PlayerSet.EMPTY);
				continue;
			}

			UnlockRequest unlockRequest = tryRequestUnlock(game, teams, room, team);
			if (room.unlockingState == null && unlockRequest.isAccepted(room)) {
				room.unlockingState = new UnlockingState(team);
			}

			updateUnlockingBar(room, team, teamState, unlockRequest);
		}
	}

	private void updateUnlockingBar(RoomInstance room, GameTeamKey team, TeamRoomInstance teamState, UnlockRequest unlockRequest) {
		GameBossBar bar = teamState.unlockingBar;
		UnlockingState unlockingState = room.unlockingState;

		if (unlockingState != null) {
			float progress = (float) unlockingState.unlockingTicks / RoomEntrancePadEntity.TOTAL_UNLOCK_TICKS;
			int percent = Math.round(progress * 100.0f);
			boolean otherTeam = !unlockingState.team.equals(team);
			BossEvent.BossBarColor color = otherTeam ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.GREEN;
			bar.setTitle(EscapeRaceTexts.UNLOCKING.apply(percent));
			bar.setProgress(progress);
			bar.setStyle(color, BossEvent.BossBarOverlay.PROGRESS);
		} else {
			if (unlockRequest.breakBucks >= room.cost) {
				int playersCrouching = unlockRequest.playersCrouching.size();
				bar.setProgress((float) playersCrouching / unlockRequest.teamSize);
				bar.setTitle(EscapeRaceTexts.LOCKED_NOT_ENOUGH_PLAYERS.apply(playersCrouching, unlockRequest.teamSize));
				bar.setStyle(BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
			} else {
				bar.setProgress((float) unlockRequest.breakBucks / room.cost);
				bar.setTitle(EscapeRaceTexts.LOCKED_CANNOT_AFFORD.apply(unlockRequest.breakBucks, room.cost));
				bar.setStyle(BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
			}
		}

		bar.setPlayers(unlockRequest.playersInRegion);
	}

	private boolean tickUnlocking(IGamePhase game, TeamState teams, RoomInstance room, UnlockingState unlockingState) {
		UnlockRequest unlockRequest = tryRequestUnlock(game, teams, room, unlockingState.team);
		TriState result = unlockingState.tick(unlockRequest.isAccepted(room));

		room.entrancePad.setUnlockingTicks(unlockingState.unlockingTicks, unlockingState.wasUnlocking);

		TeamRoomInstance teamRoom = room.getTeamRoom(unlockingState.team);
		if (result.isTrue()) {
			teamRoom.status = RoomStatus.UNLOCKED;
			game.statistics().forTeam(unlockingState.team).incrementInt(StatisticKey.BREAK_BUCKS, -room.cost);
			teamRoom.sendToSubPhase(game, teams.getPlayersForTeam(game, unlockingState.team));
		}

		return !result.isDefault();
	}

	private UnlockRequest tryRequestUnlock(IGamePhase game, TeamState teams, RoomInstance room, GameTeamKey team) {
		PlayerSet players = teams.getParticipantsForTeam(game, team);
		PlayerSet playersInRegion = players.filter(player -> room.entranceBox.contains(player.position()));
		PlayerSet playersCrouching = playersInRegion.filter(ServerPlayer::isCrouching);
		int breakBucks = game.statistics().forTeam(team).getInt(StatisticKey.BREAK_BUCKS);
		return new UnlockRequest(playersInRegion, playersCrouching, players.size(), breakBucks);
	}

	private void onGameStarted(IGamePhase game, State state) {
		ServerLevel level = game.level();
		for (RoomConfig room : rooms) {
			GameConfig subGameConfig = GameConfigs.REGISTRY.get(room.gameId);
			if (subGameConfig == null) {
				throw new GameException(Component.literal("No game config with id: " + room.gameId));
			}
			BlockBox box = game.mapRegions().getOrThrow(room.entranceRegion);
			RoomEntrancePadEntity pad = EscapeRace.ROOM_ENTRANCE_PAD.get().create(level, EntitySpawnReason.LOAD);
			if (pad == null) {
				throw new GameException(Component.literal("Could not spawn entrance pad"));
			}
			RoomInstance roomInstance = new RoomInstance(room, subGameConfig, box, pad);
			Vec3 center = box.center();
			BlockPos size = box.size();
			pad.snapTo(center.x(), box.min().getY(), center.z(), room.facing, 0.0f);
			pad.setWidth(size.getX() - 0.01f);
			pad.setHeight(size.getY());
			pad.setDepth(size.getZ() - 0.01f);
			level.addFreshEntity(pad);
			state.rooms.put(room.entranceRegion, roomInstance);

			level.getChunkSource().updateChunkForced(pad.chunkPosition(), true);
		}
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return EscapeRace.WAREHOUSE_SETUP_BEHAVIOUR;
	}

	public static class State {
		public Map<String, RoomInstance> rooms;
		public State() {
			this.rooms = new HashMap<>();
		}

		public EscapeRaceRoomsState asClientState(@Nullable GameTeamKey team) {
			Int2ObjectMap<EscapeRaceRoomsState.Room> rooms = new Int2ObjectOpenHashMap<>();
			for (RoomInstance room : this.rooms.values()) {
				TeamRoomInstance teamState = team != null ? room.getTeamRoom(team) : null;
				rooms.put(room.entrancePad.getId(), new EscapeRaceRoomsState.Room(
						room.config.displayName,
						// TODO: What do we show for spectators?
						teamState != null ? teamState.status : RoomStatus.LOCKED,
						room.cost,
						room.isBlockedFor(team)
				));
			}
			return new EscapeRaceRoomsState(rooms);
		}
	}

	public record RoomConfig(
			String entranceRegion,
			float facing,
			int baseCost,
			Component displayName,
			ResourceLocation gameId
	) {
		public static final Codec<RoomConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("entrance_region").forGetter(RoomConfig::entranceRegion),
				Codec.FLOAT.fieldOf("facing").forGetter(RoomConfig::facing),
				Codec.INT.fieldOf("cost").forGetter(RoomConfig::baseCost),
				ComponentSerialization.CODEC.fieldOf("display_name").forGetter(RoomConfig::displayName),
				ResourceLocation.CODEC.fieldOf("game").forGetter(RoomConfig::gameId)
		).apply(i, RoomConfig::new));
	}

	public static class RoomInstance {
		private final RoomConfig config;
		private final GameConfig subGameConfig;
		private final BlockBox entranceBox;
		private final RoomEntrancePadEntity entrancePad;
		private int cost;

		public final Map<GameTeamKey, TeamRoomInstance> teamRooms = new HashMap<>();
		private @Nullable UnlockingState unlockingState;

		public RoomInstance(RoomConfig config, GameConfig subGameConfig, BlockBox entranceBox, RoomEntrancePadEntity entrancePad) {
			this.config = config;
			this.subGameConfig = subGameConfig;
			this.entranceBox = entranceBox;
			this.entrancePad = entrancePad;
			cost = config.baseCost();
		}

		public TeamRoomInstance getTeamRoom(GameTeamKey teamKey) {
			return teamRooms.computeIfAbsent(teamKey, k -> new TeamRoomInstance(this));
		}

		public boolean isBlockedFor(@Nullable GameTeamKey team) {
			return unlockingState != null && !unlockingState.team.equals(team);
		}
	}

	public static class TeamRoomInstance {
		private final RoomInstance room;
		private final GameBossBar unlockingBar = new GameBossBar(CommonComponents.EMPTY, BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
		private RoomStatus status = RoomStatus.LOCKED;

		@Nullable
		private PendingSubPhase pendingSubGame;
		@Nullable
		private IGamePhase subGame;
		private boolean stopped;

		public TeamRoomInstance(RoomInstance room) {
			this.room = room;
		}

		public boolean maybeTransferPlayer(IGamePhase topGame, ServerPlayer player) {
			if (stopped) {
				return false;
			}
			if (pendingSubGame != null) {
				pendingSubGame.queuePlayer(player);
				return true;
			} else if (subGame != null) {
				topGame.transferPlayerTo(player, subGame);
				return true;
			}
			return false;
		}

		public boolean sendToSubPhase(IGamePhase topGame, PlayerSet players) {
			if (stopped) {
				return false;
			}
			if (pendingSubGame == null && subGame == null) {
				pendingSubGame = topGame.createSubPhase(room.subGameConfig);
				pendingSubGame.whenCreated(this::onGameCreated);
			}
			if (pendingSubGame != null) {
				pendingSubGame.queuePlayers(players);
			} else if (subGame != null) {
				topGame.transferPlayersTo(players, subGame);
			}
			return true;
		}

		private void onGameCreated(IGamePhase subGame, EventRegistrar subEvents) {
			pendingSubGame = null;
			this.subGame = subGame;
			subEvents.listen(GamePhaseEvents.STOP, reason -> {
				stopped = true;
				this.subGame = null;
				subGame.returnToParent(subGame.allPlayers());
			});
		}
	}

	private record UnlockRequest(
			PlayerSet playersInRegion,
			PlayerSet playersCrouching,
			int teamSize,
			int breakBucks
	) {
		public boolean isAccepted(RoomInstance room) {
			if (teamSize == 0) {
				// The team is probably in another room
				return false;
			}
			return playersCrouching.size() >= teamSize && breakBucks >= room.cost;
		}
	}

	private static class UnlockingState {
		private final GameTeamKey team;
		private int unlockingTicks;
		private boolean wasUnlocking = true;

		private UnlockingState(GameTeamKey team) {
			this.team = team;
		}

		public TriState tick(boolean unlocking) {
			wasUnlocking = unlocking;
			if (unlocking) {
				unlockingTicks++;
				if (unlockingTicks >= RoomEntrancePadEntity.TOTAL_UNLOCK_TICKS) {
					return TriState.TRUE;
				}
			} else {
				unlockingTicks--;
				if (unlockingTicks <= 0) {
					return TriState.FALSE;
				}
			}
			return TriState.DEFAULT;
		}
	}
}
