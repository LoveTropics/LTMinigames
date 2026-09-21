package org.lovetropics.games;

import com.google.common.base.Suppliers;
import com.mojang.logging.LogUtils;
import com.tterrag.registrate.providers.DataProviderInitializer;
import com.tterrag.registrate.providers.ProviderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
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
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.lovetropics.games.client.KeybindsTexts;
import org.lovetropics.games.client.render.block.TriviaChestRenderer;
import org.lovetropics.games.common.config.ConfigLT;
import org.lovetropics.games.common.content.MinigameTexts;
import org.lovetropics.games.common.content.bingo.Bingo;
import org.lovetropics.games.common.content.biodiversity_blitz.BiodiversityBlitz;
import org.lovetropics.games.common.content.biodiversity_blitz.BiodiversityBlitzTexts;
import org.lovetropics.games.common.content.block.LoveTropicsBlocks;
import org.lovetropics.games.common.content.block.TrashType;
import org.lovetropics.games.common.content.block_party.BlockParty;
import org.lovetropics.games.common.content.block_party.BlockPartyTexts;
import org.lovetropics.games.common.content.build_battle.BuildBattle;
import org.lovetropics.games.common.content.build_battle.BuildBattleTexts;
import org.lovetropics.games.common.content.build_competition.BuildCompetition;
import org.lovetropics.games.common.content.columns_of_chaos.ColumnsOfChaos;
import org.lovetropics.games.common.content.columns_of_chaos.ColumnsOfChaosTexts;
import org.lovetropics.games.common.content.connect4.ConnectFour;
import org.lovetropics.games.common.content.connect4.ConnectFourTexts;
import org.lovetropics.games.common.content.crafting_bee.CraftingBee;
import org.lovetropics.games.common.content.crafting_bee.CraftingBeeTexts;
import org.lovetropics.games.common.content.de_a_coudre.DeACoudre;
import org.lovetropics.games.common.content.drr_showdown.DDRShowdown;
import org.lovetropics.games.common.content.drr_showdown.DDRShowdownTexts;
import org.lovetropics.games.common.content.escape_race.EscapeRace;
import org.lovetropics.games.common.content.escape_race.EscapeRaceParticles;
import org.lovetropics.games.common.content.escape_race.EscapeRaceTexts;
import org.lovetropics.games.common.content.paint_party.PaintParty;
import org.lovetropics.games.common.content.paint_party.PaintPartyTexts;
import org.lovetropics.games.common.content.qottott.Qottott;
import org.lovetropics.games.common.content.qottott.QottottTexts;
import org.lovetropics.games.common.content.river_race.RiverRace;
import org.lovetropics.games.common.content.river_race.RiverRaceTexts;
import org.lovetropics.games.common.content.speed_carb_golf.SpeedCarbGolf;
import org.lovetropics.games.common.content.speed_carb_golf.SpeedCarbGolfTexts;
import org.lovetropics.games.common.content.spleef.Spleef;
import org.lovetropics.games.common.content.survive_the_tide.SurviveTheTide;
import org.lovetropics.games.common.content.survive_the_tide.SurviveTheTideTexts;
import org.lovetropics.games.common.content.survive_the_tide.biome.SurviveTheTideBiomes;
import org.lovetropics.games.common.content.survive_the_tide.entity.DriftwoodRider;
import org.lovetropics.games.common.content.trash_dive.TrashDive;
import org.lovetropics.games.common.content.trash_dive.TrashDiveTexts;
import org.lovetropics.games.common.content.treasure_dig.TreasureDig;
import org.lovetropics.games.common.content.turtle_race.RiderBehavior;
import org.lovetropics.games.common.content.turtle_race.TurtleRace;
import org.lovetropics.games.common.content.turtle_race.TurtleRaceTexts;
import org.lovetropics.games.common.core.chat.ChatChannelStore;
import org.lovetropics.games.common.core.command.LoveTropicsEntityOptions;
import org.lovetropics.games.common.core.data.LoveTropicsAttachments;
import org.lovetropics.games.common.core.entity.MinigameEntities;
import org.lovetropics.games.common.core.extension.LimitedSpawnerAttachment;
import org.lovetropics.games.common.core.game.GameLootModifier;
import org.lovetropics.games.common.core.game.behavior.GameBehaviorTypes;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import org.lovetropics.games.common.core.game.impl.GameEventDispatcher;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehaviors;
import org.lovetropics.games.common.core.game.persistent.behavior.parkour.Parkour;
import org.lovetropics.games.common.core.game.util.GameTexts;
import org.lovetropics.games.common.core.item.MinigameDataComponents;
import org.lovetropics.games.common.core.item.MinigameItems;
import org.lovetropics.games.common.core.map.VoidChunkGenerator;
import org.lovetropics.games.common.core.map.workspace.MapWorkspaceManager;
import org.lovetropics.games.common.dev.DevPackSource;
import org.lovetropics.games.common.role.StreamHosts;
import org.lovetropics.games.common.util.PredictedToggle;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;
import org.lovetropics.games.common.util.world.gamedata.GameDataAccessor;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@Mod(LoveTropics.ID)
public class LoveTropics {
	public static final Logger LOGGER = LogUtils.getLogger();

	public static final String ID = "ltminigames";

	private static final Identifier TAB_ID = LoveTropics.id("ltminigames");
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
			Bingo.KEYS.forEach(consumer);
			BiodiversityBlitzTexts.collectTranslations(consumer);
			BlockPartyTexts.KEYS.forEach(consumer);
			ColumnsOfChaosTexts.KEYS.forEach(consumer);
			BuildBattleTexts.KEYS.forEach(consumer);
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
			KeybindsTexts.collectTranslations(consumer);
			EscapeRaceTexts.collectTranslations(consumer);
			DDRShowdownTexts.KEYS.forEach(consumer);
		});

		registrate.generic(TAB_ID.getPath(), Registries.CREATIVE_MODE_TAB, () -> CreativeModeTab.builder()
				.title(registrate().addLang("itemGroup", TAB_ID, "LTMinigames"))
				.icon(() -> LoveTropicsBlocks.TRASH.get(TrashType.COLA).asStack())
				.build()
		).build();

		return registrate;
	});

	public LoveTropics(IEventBus modBus, ModContainer modContainer) {
		NeoForge.EVENT_BUS.addListener(this::onAttemptSpawn);

		// Registry objects
		LoveTropicsBlocks.init();
		MinigameItems.init();
		MinigameEntities.init();

		GameBehaviorTypes.init(modBus);
		PersistentGameBehaviors.init(modBus);
		GameClientStateTypes.init(modBus);
		StreamHosts.init();

		BuildCompetition.init();
		SurviveTheTide.init();
		TrashDive.init();
		BlockParty.init();
		Bingo.init();
		CraftingBee.init();
		ConnectFour.init();
		TurtleRace.init();
		Qottott.init();
		Spleef.init();
		BuildBattle.init();
		RiverRace.init();
		DeACoudre.init();
		TreasureDig.init();
		ColumnsOfChaos.init();
		PaintParty.init();
		SpeedCarbGolf.init();
		EscapeRace.init();
		DDRShowdown.init();

		DriftwoodRider.ATTACHMENT_TYPES.register(modBus);
		ChatChannelStore.ATTACHMENT_TYPES.register(modBus);
		RiderBehavior.ATTACHMENT_TYPES.register(modBus);
		LimitedSpawnerAttachment.ATTACHMENT_TYPES.register(modBus);
		LoveTropicsAttachments.ATTACHMENT_TYPES.register(modBus);
		SoundRegistry.REGISTER.register(modBus);
		MinigameDataComponents.REGISTER.register(modBus);
		BiodiversityBlitz.DATA_COMPONENTS.register(modBus);
		RiverRace.DATA_COMPONENTS.register(modBus);
		VoidChunkGenerator.REGISTER.register(modBus);
		EscapeRace.ENTITY_SERIALIZERS.register(modBus);
		EscapeRace.DATA_COMPONENTS.register(modBus);
		PredictedToggle.REGISTER.register(modBus);
		EscapeRaceParticles.REGISTER.register(modBus);
		Parkour.init();

		LoveTropicsEntityOptions.register();

		modContainer.registerConfig(ModConfig.Type.CLIENT, ConfigLT.CLIENT_CONFIG);
		modContainer.registerConfig(ModConfig.Type.COMMON, ConfigLT.SERVER_CONFIG);

		NeoForge.EVENT_BUS.register(new GameEventDispatcher());

		modBus.addListener(this::registerLootModifiers);

		if (!FMLEnvironment.isProduction()) {
			loadDevPacks(modBus);
		}
	}

	private void registerLootModifiers(RegisterEvent event) {
		event.register(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, id("game"), () -> GameLootModifier.CODEC);
	}

	private void loadDevPacks(IEventBus modBus) {
		Path repositoryRoot = FMLPaths.GAMEDIR.get().getParent();
		Path datapackRoot = repositoryRoot.getParent().resolve("LTDatapack");
		if (!Files.exists(datapackRoot)) {
			LOGGER.warn("Couldn't find LTDatapack repository at {}, not loading", datapackRoot.toAbsolutePath());
			return;
		}
		modBus.addListener((AddPackFindersEvent event) -> {
			MutableComponent name = event.getPackType() == PackType.CLIENT_RESOURCES ? Component.literal("LTDatapack - Assets") : Component.literal("LTDatapack");
			event.addRepositorySource(new DevPackSource(datapackRoot, event.getPackType(), "lt", name));
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

	public static Identifier id(String location) {
		return Identifier.fromNamespaceAndPath(ID, location);
	}

	private void onAttemptSpawn(MobSpawnEvent.PositionCheck event) {
		if (event.getSpawnType() == EntitySpawnReason.SPAWNER) {
			MapWorkspaceManager workspace = MapWorkspaceManager.get(event.getLevel().getServer());
			if (workspace.getWorkspace(event.getLevel().getLevel().dimension()) != null) {
				event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
			}
		}
	}

	@EventBusSubscriber(modid = ID, value = Dist.CLIENT)
	public static class ClientSetup {
		@SubscribeEvent
		public static void setupClient(FMLClientSetupEvent event) {
			BlockEntityRenderers.register(RiverRace.TRIVIA_CHEST_BLOCK_ENTITY.get(), TriviaChestRenderer::new);
		}
	}
}
