package com.lovetropics.minigames;

import com.google.common.base.Suppliers;
import com.lovetropics.minigames.client.game.handler.GameSidebarRenderer;
import com.lovetropics.minigames.client.game.handler.spectate.SpectatingUi;
import com.lovetropics.minigames.client.lobby.LobbyKeybinds;
import com.lovetropics.minigames.client.lobby.LobbyStateGui;
import com.lovetropics.minigames.client.render.block.TriviaChestRenderer;
import com.lovetropics.minigames.common.config.ConfigLT;
import com.lovetropics.minigames.common.content.MinigameTexts;
import com.lovetropics.minigames.common.content.biodiversity_blitz.BiodiversityBlitz;
import com.lovetropics.minigames.common.content.biodiversity_blitz.BiodiversityBlitzTexts;
import com.lovetropics.minigames.common.content.biodiversity_blitz.client_state.render.BbClientRenderEffects;
import com.lovetropics.minigames.common.content.block.LoveTropicsBlocks;
import com.lovetropics.minigames.common.content.block.TrashType;
import com.lovetropics.minigames.common.content.block_party.BlockParty;
import com.lovetropics.minigames.common.content.block_party.BlockPartyTexts;
import com.lovetropics.minigames.common.content.build_competition.BuildCompetition;
import com.lovetropics.minigames.common.content.columns_of_chaos.ColumnsOfChaos;
import com.lovetropics.minigames.common.content.columns_of_chaos.ColumnsOfChaosTexts;
import com.lovetropics.minigames.common.content.connect4.ConnectFour;
import com.lovetropics.minigames.common.content.connect4.ConnectFourTexts;
import com.lovetropics.minigames.common.content.crafting_bee.CraftingBee;
import com.lovetropics.minigames.common.content.crafting_bee.CraftingBeeTexts;
import com.lovetropics.minigames.common.content.de_a_coudre.DeACoudre;
import com.lovetropics.minigames.common.content.hide_and_seek.HideAndSeek;
import com.lovetropics.minigames.common.content.paint_party.PaintParty;
import com.lovetropics.minigames.common.content.paint_party.PaintPartyTexts;
import com.lovetropics.minigames.common.content.qottott.Qottott;
import com.lovetropics.minigames.common.content.qottott.QottottTexts;
import com.lovetropics.minigames.common.content.river_race.RiverRace;
import com.lovetropics.minigames.common.content.river_race.RiverRaceTexts;
import com.lovetropics.minigames.common.content.river_race.render.RiverRaceBarRenderer;
import com.lovetropics.minigames.common.content.speed_carb_golf.SpeedCarbGolf;
import com.lovetropics.minigames.common.content.speed_carb_golf.SpeedCarbGolfTexts;
import com.lovetropics.minigames.common.content.spleef.Spleef;
import com.lovetropics.minigames.common.content.survive_the_tide.SurviveTheTide;
import com.lovetropics.minigames.common.content.survive_the_tide.SurviveTheTideTexts;
import com.lovetropics.minigames.common.content.survive_the_tide.biome.SurviveTheTideBiomes;
import com.lovetropics.minigames.common.content.survive_the_tide.entity.DriftwoodRider;
import com.lovetropics.minigames.common.content.trash_dive.TrashDive;
import com.lovetropics.minigames.common.content.trash_dive.TrashDiveTexts;
import com.lovetropics.minigames.common.content.treasure_dig.TreasureDig;
import com.lovetropics.minigames.common.content.turtle_race.TurtleRace;
import com.lovetropics.minigames.common.content.turtle_race.TurtleRaceTexts;
import com.lovetropics.minigames.common.core.chat.ChatChannelStore;
import com.lovetropics.minigames.common.core.command.ChatCommand;
import com.lovetropics.minigames.common.core.command.ExtendedBossBarCommand;
import com.lovetropics.minigames.common.core.command.LoveTropicsEntityOptions;
import com.lovetropics.minigames.common.core.command.MapCommand;
import com.lovetropics.minigames.common.core.command.ParticleLineCommand;
import com.lovetropics.minigames.common.core.command.TemporaryDimensionCommand;
import com.lovetropics.minigames.common.core.command.game.CancelGameCommand;
import com.lovetropics.minigames.common.core.command.game.ExecuteAtRegionCommand;
import com.lovetropics.minigames.common.core.command.game.FinishGameCommand;
import com.lovetropics.minigames.common.core.command.game.GameActionCommand;
import com.lovetropics.minigames.common.core.command.game.GameControlCommand;
import com.lovetropics.minigames.common.core.command.game.GamePackageCommand;
import com.lovetropics.minigames.common.core.command.game.JoinGameCommand;
import com.lovetropics.minigames.common.core.command.game.LeaveGameCommand;
import com.lovetropics.minigames.common.core.command.game.ManageGameLobbyCommand;
import com.lovetropics.minigames.common.core.command.game.StartGameCommand;
import com.lovetropics.minigames.common.core.dimension.RuntimeDimensions;
import com.lovetropics.minigames.common.core.entity.MinigameEntities;
import com.lovetropics.minigames.common.core.extension.LimitedSpawnerAttachment;
import com.lovetropics.minigames.common.core.game.IGameManager;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionTargetTypes;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.game.impl.GameEventDispatcher;
import com.lovetropics.minigames.common.core.game.predicate.entity.EntityPredicates;
import com.lovetropics.minigames.common.core.game.predicate.loot.LootItemConditions;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import com.lovetropics.minigames.common.core.integration.BackendIntegrations;
import com.lovetropics.minigames.common.core.item.MinigameDataComponents;
import com.lovetropics.minigames.common.core.item.MinigameItems;
import com.lovetropics.minigames.common.core.map.VoidChunkGenerator;
import com.lovetropics.minigames.common.core.map.workspace.MapWorkspaceManager;
import com.lovetropics.minigames.common.role.StreamHosts;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;
import com.lovetropics.minigames.common.util.world.gamedata.GameDataAccessor;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import com.tterrag.registrate.providers.DataProviderInitializer;
import com.tterrag.registrate.providers.ProviderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@Mod(LoveTropics.ID)
public class LoveTropics {
	public static final Logger LOGGER = LogUtils.getLogger();

	public static final String ID = "ltminigames";

	private static final ResourceLocation TAB_ID = LoveTropics.location("ltminigames");
	public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, TAB_ID);

	private static final Supplier<LoveTropicsRegistrate> REGISTRATE = Suppliers.memoize(() -> {
		LoveTropicsRegistrate registrate = LoveTropicsRegistrate.create(ID)
				.defaultCreativeTab(ResourceKey.create(Registries.CREATIVE_MODE_TAB, TAB_ID));

		DataProviderInitializer initializer = registrate.getDataGenInitializer();
		SurviveTheTideBiomes.addTo(initializer);

		registrate.addDataGenerator(ProviderType.LANG, prov -> {
			BiConsumer<String, String> consumer = prov::add;
			GameTexts.collectTranslations(consumer);
			MinigameTexts.KEYS.forEach(consumer);
			BiodiversityBlitzTexts.collectTranslations(consumer);
			BlockPartyTexts.KEYS.forEach(consumer);
			ColumnsOfChaosTexts.KEYS.forEach(consumer);
			PaintPartyTexts.KEYS.forEach(consumer);
			SpeedCarbGolfTexts.KEYS.forEach(consumer);
			CraftingBeeTexts.KEYS.forEach(consumer);
			ConnectFourTexts.KEYS.forEach(consumer);
			SurviveTheTideTexts.KEYS.forEach(consumer);
			TrashDiveTexts.KEYS.forEach(consumer);
			TurtleRaceTexts.KEYS.forEach(consumer);
			QottottTexts.KEYS.forEach(consumer);
			RiverRaceTexts.collectTranslations(consumer);
			GameDataAccessor.KEYS.forEach(consumer);
		});

		registrate.generic(TAB_ID.getPath(), Registries.CREATIVE_MODE_TAB, () -> CreativeModeTab.builder()
				.title(registrate().addLang("itemGroup", TAB_ID, "LTMinigames"))
				.icon(() -> LoveTropicsBlocks.TRASH.get(TrashType.COLA).asStack())
				.build()
		).build();

		return registrate;
	});

	public LoveTropics(IEventBus modBus, ModContainer modContainer) {
		NeoForge.EVENT_BUS.addListener(this::onServerAboutToStart);
		NeoForge.EVENT_BUS.addListener(this::onServerStopping);
		NeoForge.EVENT_BUS.addListener(this::registerCommands);
		NeoForge.EVENT_BUS.addListener(this::onAttemptSpawn);

		modBus.addListener(ConfigLT::onLoad);
		modBus.addListener(ConfigLT::onFileChange);

		// Registry objects
		LoveTropicsBlocks.init();
		MinigameItems.init();
		MinigameEntities.init();

		GameBehaviorTypes.init(modBus);
		ActionTargetTypes.init(modBus);
		EntityPredicates.init(modBus);
		LootItemConditions.init();
		GameClientStateTypes.init(modBus);
		StreamHosts.init();

		BuildCompetition.init();
		HideAndSeek.init();
		SurviveTheTide.init();
		TrashDive.init();
		BlockParty.init();
		CraftingBee.init();
		ConnectFour.init();
		TurtleRace.init();
		Qottott.init();
		Spleef.init();
		RiverRace.init();
		DeACoudre.init();
		TreasureDig.init();
		ColumnsOfChaos.init();
		PaintParty.init();
		SpeedCarbGolf.init();

		DriftwoodRider.ATTACHMENT_TYPES.register(modBus);
		ChatChannelStore.ATTACHMENT_TYPES.register(modBus);
		LimitedSpawnerAttachment.ATTACHMENT_TYPES.register(modBus);
		SoundRegistry.REGISTER.register(modBus);
		MinigameDataComponents.REGISTER.register(modBus);
		BiodiversityBlitz.DATA_COMPONENTS.register(modBus);
		RiverRace.DATA_COMPONENTS.register(modBus);
		VoidChunkGenerator.REGISTER.register(modBus);

		LoveTropicsEntityOptions.register();

		modContainer.registerConfig(ModConfig.Type.CLIENT, ConfigLT.CLIENT_CONFIG);
		modContainer.registerConfig(ModConfig.Type.COMMON, ConfigLT.SERVER_CONFIG);

		GameEventDispatcher eventDispatcher = new GameEventDispatcher(IGameManager.get());
		NeoForge.EVENT_BUS.register(eventDispatcher);

		modBus.addListener((RegisterGuiLayersEvent event) -> {
			LobbyStateGui.registerOverlays(event);
			GameSidebarRenderer.registerOverlays(event);
			SpectatingUi.registerOverlays(event);
			BbClientRenderEffects.registerOverlays(event);
			RiverRaceBarRenderer.registerOverlays(event);
		});
	}

	private static final Pattern QUALIFIER = Pattern.compile("-\\w+\\+\\d+");

	public static String getCompatVersion() {
		return getCompatVersion(ModList.get().getModContainerById(ID).orElseThrow(IllegalStateException::new).getModInfo().getVersion().toString());
	}

	private static String getCompatVersion(String fullVersion) {
		return QUALIFIER.matcher(fullVersion).replaceAll("");
	}

	public static LoveTropicsRegistrate registrate() {
		return REGISTRATE.get();
	}

	public static ResourceLocation location(String location) {
		return ResourceLocation.fromNamespaceAndPath(ID, location);
	}

	private void registerCommands(RegisterCommandsEvent event) {
		CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
		JoinGameCommand.register(dispatcher);
		StartGameCommand.register(dispatcher);
		FinishGameCommand.register(dispatcher);
		CancelGameCommand.register(dispatcher);
		LeaveGameCommand.register(dispatcher);
		GameControlCommand.register(dispatcher);
		MapCommand.register(dispatcher);
		TemporaryDimensionCommand.register(dispatcher);
		GamePackageCommand.register(dispatcher);
		ManageGameLobbyCommand.register(dispatcher);
		ExtendedBossBarCommand.register(dispatcher);
		ParticleLineCommand.register(event.getBuildContext(), dispatcher);
		ChatCommand.register(dispatcher);
		ExecuteAtRegionCommand.register(dispatcher);
		GameActionCommand.register(dispatcher);
	}

	private void onServerAboutToStart(final ServerAboutToStartEvent event) {
		BackendIntegrations.get().sendOpen();
	}

	private void onServerStopping(final ServerStoppingEvent event) {
		BackendIntegrations.get().sendClose();
	}

	private void onAttemptSpawn(final MobSpawnEvent.PositionCheck event) {
		if (event.getSpawnType() == EntitySpawnReason.SPAWNER) {
			var workspace = MapWorkspaceManager.get(event.getLevel().getServer());
			if (workspace.getWorkspace(event.getLevel().getLevel().dimension()) != null) {
				event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
			}
		}
	}

	public static void onServerStoppingUnsafely(MinecraftServer server) {
		RuntimeDimensions.onServerStoppingUnsafely(server);
	}

	@EventBusSubscriber(modid = ID, value = Dist.CLIENT)
	public static class ClientSetup {
		@SubscribeEvent
		public static void setupClient(final FMLClientSetupEvent event) {
			LobbyKeybinds.init();
			BlockEntityRenderers.register(RiverRace.TRIVIA_CHEST_BLOCK_ENTITY.get(), TriviaChestRenderer::new);
		}
	}
}
