package com.lovetropics.minigames.gametests.api;

import com.google.common.base.CaseFormat;
import com.google.common.base.Suppliers;
import com.lovetropics.lib.permission.PermissionsApi;
import com.lovetropics.lib.permission.role.RoleLookup;
import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.datagen.BehaviorFactory;
import com.lovetropics.minigames.common.core.game.datagen.BehaviorProvider;
import com.lovetropics.minigames.common.core.game.datagen.GameProvider;
import com.mojang.serialization.MapCodec;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@EventBusSubscriber(modid = "ltminigames")
public class LTMinigamesGameTests {
	public static final TestPermissionAPI PERMISSIONS = new TestPermissionAPI();

	private static final Supplier<Map<Identifier, MinigameTest>> TESTS = Suppliers.memoize(() -> {
		final var classes = ModList.get().getAllScanData().stream()
				.flatMap(sc -> sc.getAnnotations().stream())
				.filter(an -> an.annotationType().equals(RegisterMinigameTest.TYPE))
				.map(an -> an.clazz().getInternalName())
				.toList();

		final var testMap = new HashMap<Identifier, MinigameTest>();
		try {
			for (String cls : classes) {
				final Class<?> clazz = Class.forName(cls.replace('/', '.'));
				final MinigameTest test = (MinigameTest) clazz.getDeclaredConstructor().newInstance();
				testMap.put(test.id(), test);
			}
		} catch (Exception ex) {
			LoveTropics.LOGGER.error("Could not create minigame test: ", ex);
		}

		return testMap;
	});
	public static final String TESTING_PACK = "testing";

	@SubscribeEvent
	static void register(final RegisterGameTestsEvent event) {
		for (var entry : TESTS.get().entrySet()) {
			var test = entry.getValue();
			var id = entry.getKey();
			for (Method testMethod : test.getClass().getDeclaredMethods()) {
				GameTest gametest = testMethod.getAnnotation(GameTest.class);
				if (gametest == null) {
					continue;
				}

				testMethod.setAccessible(true);

				Identifier testId = id.withPath(path -> CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, path)
						+ "/" + CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, testMethod.getName()));

				// Register a unique environment for every test, as we cannot run them in parallel right now
//				Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(testId, new TestEnvironmentDefinition() {
//					private RoleLookup lastRoleLookup;
//
//					@Override
//					public void setup(ServerLevel level) {
//						lastRoleLookup = PermissionsApi.lookup();
//						PermissionsApi.setRoleLookup(PERMISSIONS);
//					}
//
//					@Override
//					public void teardown(ServerLevel level) {
//						PermissionsApi.setRoleLookup(lastRoleLookup);
//					}
//
//					@Override
//					public MapCodec<? extends TestEnvironmentDefinition> codec() {
//						throw new UnsupportedOperationException();
//					}
//				});
//
//				var info = new TestData<>(
//						environment,
//						LoveTropics.location("empty_3x3"),
//						gametest.timeoutTicks(),
//						5,
//						true,
//						Rotation.NONE,
//						false,
//						1,
//						1,
//						false
//				);
//				event.registerTest(testId, new GameTestInstance(info) {
//					@Override
//					public void run(GameTestHelper helper) {
//						try {
//							testMethod.invoke(test, new LTGameTestHelper(helper));
//						} catch (Exception e) {
//							throw new RuntimeException(e);
//						}
//					}
//
//					@Override
//					public MapCodec<? extends GameTestInstance> codec() {
//						throw new UnsupportedOperationException();
//					}
//
//					@Override
//					protected MutableComponent typeDescription() {
//						return Component.literal("LTMinigames");
//					}
//				});
			}
		}
	}

	@SubscribeEvent
	static void gather(final GatherDataEvent.Client event) {
		final PackOutput out = event.getGenerator().getPackOutput(TESTING_PACK);

		final BehaviorFactory behaviors = new BehaviorFactory();
		event.getGenerator()
				.addProvider(true, new GameProvider(out, behaviors, event.getLookupProvider()) {
					@Override
					protected void generate(GameGenerator generator, HolderLookup.Provider holderProvider) {
						TESTS.get().forEach((key, test) -> test.generateGame(generator, behaviors, holderProvider));
					}
				});

		event.getGenerator()
				.addProvider(true, new BehaviorProvider(out, behaviors, event.getLookupProvider()));

		event.getGenerator().addProvider(true, new PackMetadataGenerator(out)
				.add(PackMetadataSection.TYPE, new PackMetadataSection(Component.literal("LTMinigames testing"), SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA))));
	}

	@SubscribeEvent
	static void addFinders(final AddPackFindersEvent event) {
		if (event.getPackType() == PackType.SERVER_DATA) {
			PackLocationInfo info = new PackLocationInfo(TESTING_PACK, Component.literal("testing"), PackSource.BUILT_IN, Optional.empty());
			final var resources = new PathPackResources(info, ModList.get()
					.getModContainerById(LoveTropics.ID).orElseThrow()
					.getModInfo().getOwningFile()
					.getFile().findResource(TESTING_PACK));
			event.addRepositorySource(onLoad -> onLoad.accept(Pack.readMetaAndCreate(
					info, new Pack.ResourcesSupplier() {
						@Override
						public PackResources openPrimary(PackLocationInfo pLocation) {
							return resources;
						}

						@Override
						public PackResources openFull(PackLocationInfo pLocation, Pack.Metadata pMetadata) {
							return resources;
						}
					}, PackType.SERVER_DATA, new PackSelectionConfig(true, Pack.Position.TOP, false)
			)));
		}
	}
}
