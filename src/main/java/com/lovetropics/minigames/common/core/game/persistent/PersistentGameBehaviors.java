package com.lovetropics.minigames.common.core.game.persistent;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.persistent.behavior.CompositePersistentBehavior;
import com.lovetropics.minigames.common.core.game.persistent.behavior.GameRegionBehavior;
import com.lovetropics.minigames.common.core.game.persistent.behavior.MakeSpeedyBehavior;
import com.lovetropics.minigames.common.core.game.persistent.behavior.NoOpBehavior;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class PersistentGameBehaviors {
	public static final ResourceKey<Registry<PersistentGameBehaviorType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(LoveTropics.location("persistent_behaviors"));

	public static final DeferredRegister<PersistentGameBehaviorType<?>> REGISTER = DeferredRegister.create(REGISTRY_KEY, LoveTropics.ID);
	public static final Registry<PersistentGameBehaviorType<?>> REGISTRY = REGISTER.makeRegistry(builder -> builder.sync(false));
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final Codec<PersistentGameBehaviorType<?>> TYPE_CODEC = Codec.lazyInitialized(() -> REGISTRY.byNameCodec());

	// Behaviors
	public static final PersistentGameBehaviorEntry<NoOpBehavior> NOOP = register("noop", NoOpBehavior.CODEC);
	public static final PersistentGameBehaviorEntry<CompositePersistentBehavior> COMPOSITE = register("composite", CompositePersistentBehavior.MAP_CODEC);
	public static final PersistentGameBehaviorEntry<GameRegionBehavior> GAME_REGION = register("game_region", GameRegionBehavior.CODEC);
	public static final PersistentGameBehaviorEntry<MakeSpeedyBehavior> MAKE_SPEEDY = register("make_speedy", MakeSpeedyBehavior.CODEC);

	public static <T extends PersistentGameBehavior> PersistentGameBehaviorEntry<T> register(final String name, final MapCodec<T> codec) {
		return REGISTRATE.object(name).persistentBehavior(codec).register();
	}

	public static void init(IEventBus modBus) {
		REGISTER.register(modBus);
	}
}
