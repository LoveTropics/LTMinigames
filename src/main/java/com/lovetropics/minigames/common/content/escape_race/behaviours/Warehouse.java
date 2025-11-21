package com.lovetropics.minigames.common.content.escape_race.behaviours;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.content.escape_race.EscapeRaceTexts;
import com.lovetropics.minigames.common.content.escape_race.misc.RoomEntrancePadEntity;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.GameStopReason;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.PendingSubPhase;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.command.GameCommandRegistrar;
import com.lovetropics.minigames.common.core.game.config.GameConfig;
import com.lovetropics.minigames.common.core.game.config.GameConfigs;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.IGameState;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.lovetropics.minigames.common.core.game.util.GameBossBar;
import com.lovetropics.minigames.common.core.game.util.GameWidgets;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class Warehouse implements IGameState {
	public static final int FADE_DURATION = SharedConstants.TICKS_PER_SECOND;

	private final IGamePhase topGame;
	private final TeamState teams;
	private final GameWidgets widgets;

	public final Map<String, RoomInstance> rooms = new HashMap<>();

	public Warehouse(IGamePhase topGame, TeamState teams, GameWidgets widgets, Map<String, WarehouseSetupBehaviour.RoomConfig> roomConfigs) {
		this.topGame = topGame;
		this.teams = teams;
		this.widgets = widgets;

		for (Map.Entry<String, WarehouseSetupBehaviour.RoomConfig> entry : roomConfigs.entrySet()) {
			WarehouseSetupBehaviour.RoomConfig room = entry.getValue();
			GameConfig subGameConfig = GameConfigs.REGISTRY.get(room.gameId());
			if (subGameConfig == null) {
				throw new GameException(Component.literal("No game config with id: " + room.gameId()));
			}
			Optional<String> entrance = room.entranceRegion();
			BlockBox box = entrance.map(e -> topGame.mapRegions().getOrThrow(e)).orElse(null);

			rooms.put(entry.getKey(), new RoomInstance(room, subGameConfig, box));
		}
	}

	private UnlockRequest tryRequestUnlock(IGamePhase game, TeamState teams, RoomInstance room, GameTeamKey team) {
		PlayerSet players = teams.getParticipantsForTeam(game, team);
		PlayerSet playersInRegion = players.filter(player -> room.entranceBox.contains(player.position()));
		PlayerSet playersCrouching = playersInRegion.filter(ServerPlayer::isCrouching);
		int breakBucks = game.statistics().forTeam(team).getInt(StatisticKey.BREAK_BUCKS);
		return new UnlockRequest(playersInRegion, playersCrouching, players.size(), breakBucks);
	}

	public void tick() {
		for (RoomInstance room : rooms.values()) {
			room.state = room.state.tick();
		}
	}

	public void onPlayerJoin(ServerPlayer player) {
		GameTeamKey team = teams.getTeamForPlayer(player);
		if (team == null) {
			return;
		}
		for (RoomInstance room : rooms.values()) {
			if (room.maybeTransferPlayer(player, team)) {
				return;
			}
		}
	}

	private void onRoomCreated(IGamePhase subGame, EventRegistrar subEvents, @Nullable GameTeamKey team) {
		subEvents.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) ->
				registerCommands(subGame, commands)
		);

		if (team != null) {
			PlayingRoomState.copyStatisticForTeam(topGame, subGame, team, List.of(StatisticKey.VACATION_DAYS));
		}
	}

	public void registerCommands(IGamePhase game, GameCommandRegistrar commands) {
		commands.register(Commands.literal("room")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.argument("room", StringArgumentType.word())
						.suggests((context, builder) ->
								SharedSuggestionProvider.suggest(rooms.keySet().stream(), builder)
						)
						.then(Commands.literal("cost").then(Commands.literal("set")
								.then(Commands.argument("cost", IntegerArgumentType.integer(0))
										.executes(context -> {
											RoomInstance room = getRoomArgument(context);
											room.setCost(IntegerArgumentType.getInteger(context, "cost"));
											return 1;
										})
								)
						))
						.then(Commands.literal("reset")
								.executes(context -> {
									RoomInstance room = getRoomArgument(context);
									room.state.close();
									room.state = new LockedRoomState(room);
									return 1;
								})
						)
						.then(Commands.literal("remove")
								.executes(context -> {
									RoomInstance room = getRoomArgument(context);
									removeRoom(room);
									return 1;
								})
						)
						.then(Commands.literal("join")
								.executes(context -> {
									RoomInstance room = getRoomArgument(context);
									GameTeamKey unlockingTeam = room.config.allTeams() ? null : teams.getTeamForPlayer(context.getSource().getPlayer());
									joinIntoRoom(room, unlockingTeam);
									return 1;
								})
						)
				)

		);
	}

	public void removeRooms(List<String> roomNames) {
		for (String room : roomNames) {
			RoomInstance roomInstance = rooms.get(room);
			if (roomInstance != null && !(roomInstance.state instanceof CompletedRoomState)) {
				roomInstance.state.close();
				roomInstance.state = new CompletedRoomState(null);
			}
		}
	}

	public void removeRoom(RoomInstance room) {
		room.state.close();
		room.state = new CompletedRoomState(null);
	}

	public void joinIntoRoom(RoomInstance room, @Nullable GameTeamKey team) {
		room.state.close();

		PlayingRoomState playingRoom = new PlayingRoomState(team, room);
		playingRoom.sendToSubPhase(topGame, team == null ? topGame.allPlayers() : teams.getPlayersForTeam(topGame, team));

		if (team != null) {
			topGame.statistics().forTeam(team).incrementInt(StatisticKey.BREAK_BUCKS, -room.cost);
		}

		room.state = playingRoom;
	}

	private RoomInstance getRoomArgument(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		String roomId = StringArgumentType.getString(context, "room");
		Warehouse.RoomInstance room = rooms.get(roomId);
		if (room == null) {
			throw new SimpleCommandExceptionType(Component.literal("No room with id: " + roomId)).create();
		}
		return room;
	}

	public class RoomInstance {
		private final WarehouseSetupBehaviour.RoomConfig config;
		private final GameConfig subGameConfig;
		private final @Nullable BlockBox entranceBox;

		private int cost;

		private RoomState state;

		public RoomInstance(WarehouseSetupBehaviour.RoomConfig config, GameConfig subGameConfig, @Nullable BlockBox entranceBox) {
			this.config = config;
			this.subGameConfig = subGameConfig;
			this.entranceBox = entranceBox;
			cost = config.baseCost();
			if (entranceBox != null) {
				state = new LockedRoomState(this);
			} else {
				state = new EmptyRoomState();
			}
		}

		public void setCost(int cost) {
			this.cost = cost;
		}

		public boolean maybeTransferPlayer(ServerPlayer player, GameTeamKey team) {
			if (state instanceof PlayingRoomState playingRoom && team.equals(playingRoom.team)) {
				playingRoom.transferPlayer(topGame, player);
				return true;
			}
			return false;
		}

		public boolean isPlayingRoom() {
			return state instanceof PlayingRoomState;
		}

		public void earlyExit(){
			if(state instanceof PlayingRoomState playingRoom) {
				playingRoom.close();
			}
		}

		public RoomState getState() {
			return state;
		}

		@Nullable
		public GameTeamKey getTeam() {
			if(state instanceof PlayingRoomState playingRoom) {
				return playingRoom.team;
			}
			return null;
		}
	}

	public sealed interface RoomState {
		RoomState tick();

		void close();
	}

	// Used for room 7's default behavior, since it is manual trigger
	public static final class EmptyRoomState implements RoomState {

		@Override
		public RoomState tick() {
			return this;
		}

		@Override
		public void close() {

		}
	}

	public final class LockedRoomState implements RoomState {
		private final RoomInstance room;
		private final RoomEntrancePadEntity pad;

		private @Nullable UnlockingState unlockingState;
		private final Map<GameTeamKey, GameBossBar> unlockingBars = new HashMap<>();

		public LockedRoomState(RoomInstance room) {
			this.room = room;
			RoomEntrancePadEntity pad = EscapeRace.ROOM_ENTRANCE_PAD.get().create(topGame.level(), EntitySpawnReason.LOAD);
			if (pad == null) {
				throw new GameException(Component.literal("Could not spawn entrance pad"));
			}
			this.pad = pad;
			BlockBox box = room.entranceBox;
			if (box == null) {
				throw new GameException(Component.literal("Regular room must have entrance!"));
			}
			Vec3 center = box.center();
			BlockPos size = box.size();
			pad.snapTo(center.x(), box.min().getY(), center.z(), room.config.facing(), 0.0f);
			pad.setWidth(size.getX() - 0.01f);
			pad.setHeight(size.getY());
			pad.setDepth(size.getZ() - 0.01f);
			pad.setCost(room.cost);
			pad.setRoomName(room.config.displayName());
			topGame.level().getChunkSource().updateChunkForced(pad.chunkPosition(), true);
			topGame.level().addFreshEntity(pad);
		}

		@Override
		public RoomState tick() {
			pad.setCost(room.cost);

			if (unlockingState != null) {
				GameTeamKey unlockingTeam = unlockingState.team;
				UnlockRequest unlockRequest = tryRequestUnlock(topGame, teams, room, unlockingTeam);

				TriState result = unlockingState.tick(unlockRequest.isAccepted(room));
				pad.setUnlockingTicks(unlockingState.unlockingTicks, unlockingState.wasUnlocking);

				if (result.isTrue()) {
					PlayingRoomState playingRoom = unlockGame(unlockingTeam);
					playingRoom.sendToSubPhase(topGame, teams.getPlayersForTeam(topGame, unlockingTeam));
					topGame.statistics().forTeam(unlockingTeam).incrementInt(StatisticKey.BREAK_BUCKS, -room.cost);
					return playingRoom;
				} else if (result.isFalse()) {
					unlockingState = null;
				}
			} else {
				pad.setUnlockingTicks(0, false);
			}

			for (GameTeamKey team : teams.getTeamKeys()) {
				UnlockRequest unlockRequest = tryRequestUnlock(topGame, teams, room, team);
				if (unlockingState == null && unlockRequest.isAccepted(room)) {
					unlockingState = new UnlockingState(team);
				}
				updateUnlockingBar(team, unlockRequest);
			}

			return this;
		}

		@Override
		public void close() {
			pad.discard();
			unlockingBars.values().forEach(GameBossBar::close);
			unlockingBars.clear();
		}

		private GameBossBar getOrCreateUnlockingBar(GameTeamKey team) {
			return unlockingBars.computeIfAbsent(team, key ->
					widgets.openBossBar(CommonComponents.EMPTY, BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS)
			);
		}

		private void updateUnlockingBar(GameTeamKey team, UnlockRequest unlockRequest) {
			GameBossBar bar = getOrCreateUnlockingBar(team);

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

		private PlayingRoomState unlockGame(GameTeamKey team) {
			close();
			return new PlayingRoomState(team, room);
		}
	}

	public final class PlayingRoomState implements RoomState {
		private final RoomInstance room;
		private final @Nullable GameTeamKey team; // null for non team rooms, i.e. 7
		private final PendingSubPhase pendingSubGame;
		private @Nullable IGamePhase subGame;
		private @Nullable GameStopReason stopReason;

		public PlayingRoomState(@Nullable GameTeamKey team, RoomInstance room) {
			this.room = room;
			this.team = team;
			pendingSubGame = topGame.createSubPhase(room.subGameConfig);
			pendingSubGame.whenCreated(this::onGameCreated);
			pendingSubGame.whenErrored(exception -> onGameErrored(topGame));
		}

		@Override
		public RoomState tick() {
			if (stopReason != null) {
				if (stopReason.isFinished()) {
					return new CompletedRoomState(team);
				} else {
					// Some kind of error? Give the team back their Break Bucks!
					if (team != null) {
						topGame.statistics().forTeam(team).incrementInt(StatisticKey.BREAK_BUCKS, room.cost);
					}
					return new LockedRoomState(room);
				}
			}
			return this;
		}

		public void sendToSubPhase(IGamePhase topGame, PlayerSet players) {
			players.fadeToBlack(FADE_DURATION);
			topGame.scheduler().runAfterTicks(FADE_DURATION, () -> {
				if (subGame != null) {
					topGame.transferPlayersTo(players, subGame);
				} else {
					pendingSubGame.queuePlayers(players);
				}
			});
		}

		public void transferPlayer(IGamePhase topGame, ServerPlayer player) {
			PlayerSet.of(player).fadeToBlack(FADE_DURATION);
			if (subGame != null) {
				topGame.transferPlayerTo(player, subGame);
			} else {
				pendingSubGame.queuePlayer(player);
			}
		}

		private void onGameCreated(IGamePhase subGame, EventRegistrar subEvents) {
			this.subGame = subGame;
			Map<UUID, Map<Integer, ItemStack>> stacks = new HashMap<>();

			// Copy inventory for room 7
			if (room.config.copyInventory()) {
				for (ServerPlayer participant : topGame.participants()) {
					Map<Integer, ItemStack> inv = new HashMap<>();
					for (int i = 0; i < participant.getInventory().getContainerSize(); i++) {
						inv.put(i, participant.getInventory().getItem(i).copy());
					}

					stacks.put(participant.getUUID(), inv);
				}
			}

			subEvents.listen(GamePlayerEvents.ADD, player -> {
				PlayerSet.of(player).fadeFromBlack(FADE_DURATION);

				// Set inventory slots if we have them
				Map<Integer, ItemStack> inv = stacks.get(player.getUUID());
				if (inv != null) {
					for (Map.Entry<Integer, ItemStack> e : inv.entrySet()) {
						if (e.getValue().isEmpty()) {
							continue;
						}

						player.getInventory().add(e.getKey(), e.getValue());
					}
				}
			});

			onRoomCreated(subGame, subEvents, team);

			subEvents.listen(GamePhaseEvents.STOP, reason -> {
				this.subGame = null;
				subGame.allPlayers().fadeToBlack(FADE_DURATION);
				subGame.returnToParent(subGame.allPlayers());
				stopReason = reason;

				if (team != null) {
					PlayingRoomState.copyStatisticForTeam(subGame, topGame, team, List.of(StatisticKey.VACATION_DAYS));
				}
			});
		}

		private static void copyStatisticForTeam(IGamePhase from, IGamePhase to, GameTeamKey teamKey, List<StatisticKey<?>> statisticKeys) {
			to.statistics().forTeam(teamKey).copyFrom(from.statistics().forTeam(teamKey), statisticKeys);
		}

		private void onGameErrored(IGamePhase topGame) {
			subGame = null;
			topGame.allPlayers().sendMessage(Component.literal("An error occurred starting the last room"));
			topGame.allPlayers().fadeFromBlack(FADE_DURATION);
			stopReason = GameStopReason.canceled();
		}

		@Override
		public void close() {
			if (subGame != null) {
				subGame.requestStop(GameStopReason.canceled());
				subGame = null;
			}
		}
	}

	public record CompletedRoomState(@Nullable GameTeamKey team) implements RoomState {
		@Override
		public RoomState tick() {
			return this;
		}

		@Override
		public void close() {
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
