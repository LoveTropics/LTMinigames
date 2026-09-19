package com.lovetropics.minigames.common.core.game;

import com.lovetropics.lib.slideshow.SlideshowApi;
import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.util.LTGameTestFakePlayer;
import com.lovetropics.minigames.common.util.duck.PrimaryLevelDataAccess;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class PlayerIsolation {
	public static final PlayerIsolation INSTANCE = new PlayerIsolation();

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final String ISOLATED_TAG = LoveTropics.ID + ".isolated";
	// Checked by JEDI to avoid incorrect join/leave messages
	private static final String RELOADING_TAG = LoveTropics.ID + ".reloading";

	private final Set<UUID> reloadingPlayers = new ObjectOpenHashSet<>();

	private PlayerIsolation() {
	}

	/// Player going into a GamePhase
	/// Saves player data and then teleports them
	public ServerPlayer teleportTo(ServerPlayer player, ServerLevel newLevel, Vec3 position, float yRot, float xRot) {
		TransferableState transferableState = TransferableState.copyOf(player);
		return reloadPlayer(player, newLevel, newPlayer -> {
			newPlayer.snapTo(position.x, position.y, position.z, yRot, xRot);
			newPlayer.addTag(ISOLATED_TAG);
			transferableState.restore(newPlayer);
		});
	}

	/// Player is headed back to the main event world most likely
	public ServerPlayer restore(ServerPlayer player) {
		if (isIsolated(player)) {
			return reloadPlayerFromDisk(player);
		}
		return player;
	}

	private ServerPlayer reloadPlayerFromDisk(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		PlayerList playerList = server.getPlayerList();
		Optional<CompoundTag> playerTag = playerList.loadPlayerData(player.nameAndId());

		try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(player.problemPath(), LOGGER)) {
			Optional<ValueInput> input = playerTag.map(tag -> TagValueInput.create(reporter, server.registryAccess(), tag));

			ServerLevel newLevel = input.flatMap(i -> i.read(ServerPlayer.SavedPosition.MAP_CODEC))
					.flatMap(ServerPlayer.SavedPosition::dimension)
					.map(server::getLevel)
					.orElse(server.overworld());

			return reloadPlayer(player, newLevel, newPlayer ->
					input.ifPresent(newPlayer::load)
			);
		}
	}

	public ServerPlayer reloadPlayerFromTag(CompoundTag tag, ServerPlayer player) {
		MinecraftServer server = player.level().getServer();

		try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(player.problemPath(), LOGGER)) {
			ValueInput input = TagValueInput.create(reporter, server.registryAccess(), tag);

			ServerLevel newLevel = input.read(ServerPlayer.SavedPosition.MAP_CODEC)
					.flatMap(ServerPlayer.SavedPosition::dimension)
					.map(server::getLevel)
					.orElse(server.overworld());

			return reloadPlayer(player, newLevel, newPlayer -> {
				newPlayer.load(input);
				newPlayer.addTag(ISOLATED_TAG);
			});
		}
	}

	private ServerPlayer reloadPlayer(ServerPlayer oldPlayer, ServerLevel newLevel, Consumer<ServerPlayer> initializer) {
		if (oldPlayer instanceof LTGameTestFakePlayer) {
			initializer.accept(oldPlayer);
			return oldPlayer;
		}

		MinecraftServer server = oldPlayer.level().getServer();
		PlayerList playerList = server.getPlayerList();

		reloadingPlayers.add(oldPlayer.getUUID());
		oldPlayer.addTag(RELOADING_TAG);

		ServerPlayer newPlayer = recreatePlayer(oldPlayer, newLevel);
		SlideshowApi.replacePlayer(oldPlayer, newPlayer);

		EventHooks.firePlayerLoggedOut(oldPlayer);

		// Only called once - when player enters the first game phase they enter
		// this way, we only save to disk once
		if (!isIsolated(oldPlayer)) {
			((PlayerListAccess) playerList).ltminigames$save(oldPlayer);
			// Usually we don't save+load in singleplayer within a single session, so the last singleplayer ID would only get updated upon saving
			if (server.isSingleplayerOwner(oldPlayer.nameAndId()) && server.getWorldData() instanceof PrimaryLevelDataAccess levelData) {
				levelData.ltminigames$setSinglePlayerUUID(oldPlayer.getUUID());
			}
		}

		oldPlayer.unRide();
		oldPlayer.level().removePlayerImmediately(oldPlayer, Entity.RemovalReason.DISCARDED);
		((PlayerListAccess) playerList).ltminigames$remove(oldPlayer);
		((PlayerListAccess) playerList).ltminigames$firePlayerLoading(newPlayer);

		initializer.accept(newPlayer);
		newPlayer.addTag(RELOADING_TAG);
		newPlayer.onUpdateAbilities();

		LevelData levelData = newLevel.getLevelData();
		newPlayer.connection.send(new ClientboundRespawnPacket(
				newPlayer.createCommonSpawnInfo(newLevel),
				(byte) 0
		));
		newPlayer.connection.teleport(newPlayer.getX(), newPlayer.getY(), newPlayer.getZ(), newPlayer.getYRot(), newPlayer.getXRot());
		newPlayer.connection.send(new ClientboundSetDefaultSpawnPositionPacket(newLevel.getRespawnData()));
		newPlayer.connection.send(new ClientboundChangeDifficultyPacket(levelData.getDifficulty(), levelData.isDifficultyLocked()));

		sendGameRules(newPlayer, newLevel.getGameRules());

		newLevel.addRespawnedPlayer(newPlayer);

		playerList.sendPlayerPermissionLevel(newPlayer);
		playerList.sendActivePlayerEffects(newPlayer);
		playerList.sendLevelInfo(newPlayer, newLevel);
		playerList.sendAllPlayerInfo(newPlayer);

		newPlayer.initInventoryMenu();
		newPlayer.setHealth(newPlayer.getHealth());
		newPlayer.connection.send(new ClientboundSetHealthPacket(newPlayer.getHealth(), newPlayer.getFoodData().getFoodLevel(), newPlayer.getFoodData().getSaturationLevel()));
		// It's not possible to specify to the client to drop its base attributes anymore - so just resync everything
		resyncAttributes(oldPlayer, newPlayer);

		((PlayerListAccess) playerList).ltminigames$add(newPlayer);

		playerList.broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, newPlayer));

		EventHooks.firePlayerLoggedIn(newPlayer);
		ResourceKey<Level> oldDimension = oldPlayer.level().dimension();
		ResourceKey<Level> newDimension = newLevel.dimension();
		if (oldDimension != newDimension) {
			EventHooks.firePlayerChangedDimensionEvent(newPlayer, oldDimension, newDimension);
		}

		newPlayer.removeTag(RELOADING_TAG);
		reloadingPlayers.remove(oldPlayer.getUUID());

		return newPlayer;
	}

	private static void resyncAttributes(ServerPlayer oldPlayer, ServerPlayer newPlayer) {
		AttributeMap oldAttributes = oldPlayer.getAttributes();
		AttributeMap newAttributes = newPlayer.getAttributes();

		// We need to initialize the attributes before we can synchronize them
		for (AttributeInstance instance : oldAttributes.getSyncableAttributes()) {
			newAttributes.getInstance(instance.getAttribute());
		}

		newPlayer.connection.send(new ClientboundUpdateAttributesPacket(newPlayer.getId(), newAttributes.getSyncableAttributes()));
		newAttributes.getAttributesToSync().clear();
	}

	private static ServerPlayer recreatePlayer(ServerPlayer oldPlayer, ServerLevel newLevel) {
		ServerPlayer newPlayer = new ServerPlayer(oldPlayer.level().getServer(), newLevel, oldPlayer.getGameProfile(), oldPlayer.clientInformation());
		newPlayer.connection = oldPlayer.connection;
		newPlayer.connection.player = newPlayer;
		newPlayer.setId(oldPlayer.getId());
		if (oldPlayer.getChatSession() != null) {
			newPlayer.setChatSession(oldPlayer.getChatSession());
		}
		newPlayer.updateOptions(oldPlayer.clientInformation());

		return newPlayer;
	}

	private static void sendGameRules(ServerPlayer player, GameRules gameRules) {
		boolean immediateRespawn = gameRules.get(GameRules.IMMEDIATE_RESPAWN);
		player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.IMMEDIATE_RESPAWN, immediateRespawn ? 1.0f : 0.0F));
	}

	public boolean isIsolated(ServerPlayer player) {
		return player.entityTags().contains(ISOLATED_TAG);
	}

	public boolean isReloading(ServerPlayer player) {
		return reloadingPlayers.contains(player.getUUID());
	}

	// State that can be transferred into isolation, but not back out
	private record TransferableState(
	) {
		public static TransferableState copyOf(ServerPlayer player) {
			return new TransferableState();
		}

		public void restore(ServerPlayer player) {
		}
	}
}
