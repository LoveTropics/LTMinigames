package com.lovetropics.minigames.common.dev;

import com.lovetropics.minigames.common.core.game.config.GameConfig;
import com.lovetropics.minigames.common.core.game.config.GameConfigs;
import com.lovetropics.minigames.common.core.game.impl.GameLobby;
import com.lovetropics.minigames.common.core.game.impl.GameLobbyManager;
import com.lovetropics.minigames.common.core.game.lobby.LobbyControls;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.util.Scheduler;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPresets;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.function.Function;

@EventBusSubscriber(Dist.CLIENT)
public class DevQuickPlay {
	public static final String OPTION_NAME = "quickPlayMinigame";
	private static final String LEVEL_NAME = "Minigames";

	private static final Logger LOGGER = LogUtils.getLogger();

	private static boolean firstTitleScreen;
	@Nullable
	private static ResourceLocation quickPlayGameId;

	public static void setQuickPlayGameId(@Nullable ResourceLocation quickPlayGameId) {
		DevQuickPlay.quickPlayGameId = quickPlayGameId;
	}

	public static boolean isEnabled() {
		return quickPlayGameId != null;
	}

	@Nullable
	public static GameConfig getQuickPlayGame() {
		if (quickPlayGameId == null) {
			return null;
		}
		GameConfig gameConfig = GameConfigs.REGISTRY.get(quickPlayGameId);
		if (gameConfig == null) {
			LOGGER.error("No game config with id {}", quickPlayGameId);
		}
		return gameConfig;
	}

	private static void loadIntoQuickPlayWorld() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.getLevelSource().levelExists(LEVEL_NAME)) {
			minecraft.createWorldOpenFlows().openWorld(LEVEL_NAME, () -> minecraft.setScreen(new TitleScreen()));
		} else {
			GameRules gameRules = new GameRules(FeatureFlags.VANILLA_SET);
			gameRules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
			gameRules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);

			WorldDataConfiguration dataConfiguration = new WorldDataConfiguration(DataPackConfig.DEFAULT, FeatureFlags.VANILLA_SET);
			LevelSettings levelSettings = new LevelSettings(LEVEL_NAME, GameType.CREATIVE, false, Difficulty.NORMAL, true, gameRules, dataConfiguration);
			WorldOptions worldOptions = new WorldOptions(0, false, false);
			Function<HolderLookup.Provider, WorldDimensions> dimensionsProvider = registries -> {
				Holder.Reference<WorldPreset> flatPreset = registries.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT);
				Holder.Reference<FlatLevelGeneratorPreset> redstoneReady = registries.lookupOrThrow(Registries.FLAT_LEVEL_GENERATOR_PRESET).getOrThrow(FlatLevelGeneratorPresets.REDSTONE_READY);
				return flatPreset.value().createWorldDimensions()
						.replaceOverworldGenerator(registries, new FlatLevelSource(redstoneReady.value().settings()));
			};

			minecraft.createWorldOpenFlows().createFreshLevel(LEVEL_NAME, levelSettings, worldOptions, dimensionsProvider, new TitleScreen());
		}
	}

	@SubscribeEvent
	public static void onSetScreen(ScreenEvent.Opening event) {
		if (firstTitleScreen) {
			return;
		}
		if (event.getNewScreen() instanceof TitleScreen) {
			if (isEnabled()) {
				loadIntoQuickPlayWorld();
				event.setCanceled(true);
			}
			firstTitleScreen = true;
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || !player.getServer().isSingleplayerOwner(player.getGameProfile())) {
			return;
		}
		GameConfig gameConfig = getQuickPlayGame();
		if (gameConfig == null) {
			return;
		}
		try {
			GameLobby lobby = GameLobbyManager.get().createGameLobby("Lobby", player).orElseThrow();
			lobby.getPlayers().join(player, PlayerRole.PARTICIPANT);
			lobby.getGameQueue().enqueue(gameConfig);
			Scheduler.nextTick().execute(() -> {
				LobbyControls.Action play = lobby.getControls().get(LobbyControls.Type.PLAY);
				if (play != null) {
					play.run();
				}
			});
		} catch (CommandSyntaxException e) {
			LOGGER.error("Failed to start lobby", e);
		}
	}
}
