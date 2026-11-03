package org.lovetropics.games.common.core.game.persistent;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.game.persistent.behavior.CloningMachinePersistentGame;
import org.lovetropics.games.common.core.game.persistent.behavior.CompositePersistentBehavior;
import org.lovetropics.games.common.core.game.persistent.behavior.GameRegionBehavior;
import org.lovetropics.games.common.core.game.persistent.behavior.MakeSpeedyBehavior;
import org.lovetropics.games.common.core.game.persistent.behavior.NoOpBehavior;
import org.lovetropics.games.common.core.game.persistent.behavior.RaceTrackPersistentGame;
import org.lovetropics.games.common.core.game.persistent.behavior.crab.CrabGolfClubsBehavior;
import org.lovetropics.games.common.core.game.persistent.behavior.crab.CrabGolfCrabBehavior;
import org.lovetropics.games.common.core.game.persistent.behavior.crab.CrabGolfHoleBehavior;
import org.lovetropics.games.common.core.game.persistent.behavior.crab.CrabGolfWinBehavior;
import org.lovetropics.games.common.core.game.persistent.behavior.dropper.DropperBehavior;
import org.lovetropics.games.common.core.game.persistent.behavior.parkour.ParkourBehavior;
import org.lovetropics.games.common.core.game.persistent.behavior.scavengerhunt.ScavengerHuntBehavior;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

public class PersistentGameBehaviors {
	public static final ResourceKey<Registry<MapCodec<? extends PersistentGameBehavior>>> REGISTRY_KEY = ResourceKey.createRegistryKey(LoveTropics.id("persistent_behaviors"));

	public static final DeferredRegister<MapCodec<? extends PersistentGameBehavior>> REGISTER = DeferredRegister.create(REGISTRY_KEY, LoveTropics.ID);
	public static final Registry<MapCodec<? extends PersistentGameBehavior>> REGISTRY = REGISTER.makeRegistry(builder -> builder.sync(false));
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final Codec<MapCodec<? extends PersistentGameBehavior>> TYPE_CODEC = Codec.lazyInitialized(() -> REGISTRY.byNameCodec());

	static {
		register("noop", NoOpBehavior.CODEC);
		register("composite", CompositePersistentBehavior.MAP_CODEC);
		register("game_region", GameRegionBehavior.CODEC);
		register("make_speedy", MakeSpeedyBehavior.CODEC);
		register("crab_golf_hole", CrabGolfHoleBehavior.CODEC);
		register("crab_golf_clubs", CrabGolfClubsBehavior.CODEC);
		register("crab_golf_crab", CrabGolfCrabBehavior.CODEC);
		register("crab_golf_win", CrabGolfWinBehavior.CODEC);
		register("parkour", ParkourBehavior.CODEC);
		register("dropper", DropperBehavior.CODEC);
		register("race_track", RaceTrackPersistentGame.CODEC);
		register("cloning_machine", CloningMachinePersistentGame.CODEC);
		register("scavenger_hunt", ScavengerHuntBehavior.CODEC);
	}

	private static <T extends PersistentGameBehavior> PersistentGameBehaviorEntry<T> register(String name, MapCodec<T> codec) {
		return REGISTRATE.object(name).persistentBehavior(codec).register();
	}

	public static void init(IEventBus modBus) {
		REGISTER.register(modBus);
	}
}
