package com.lovetropics.minigames.common.core.game.behavior.action;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ActionTargetTypes {
	public static final ResourceKey<Registry<Codec<? extends ActionTarget<?>>>> REGISTRY_KEY = ResourceKey.createRegistryKey(LoveTropics.location("action_target_types"));
	public static final DeferredRegister<Codec<? extends ActionTarget<?>>> REGISTER = DeferredRegister.create(REGISTRY_KEY, LoveTropics.ID);

	public static final Registry<Codec<? extends ActionTarget<?>>> REGISTRY = REGISTER.makeRegistry(builder -> builder.sync(false));

	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final DeferredHolder<Codec<? extends ActionTarget<?>>, Codec<EntityActionTarget>> ENTITY = register("entity", EntityActionTarget.CODEC);
	public static final DeferredHolder<Codec<? extends ActionTarget<?>>, Codec<PlayerActionTarget>> PLAYER = register("player", PlayerActionTarget.CODEC);
	public static final DeferredHolder<Codec<? extends ActionTarget<?>>, Codec<PlotActionTarget>> PLOT = register("plot", PlotActionTarget.CODEC);
	public static final DeferredHolder<Codec<? extends ActionTarget<?>>, Codec<TeamActionTarget>> TEAM = register("team", TeamActionTarget.CODEC);
	public static final DeferredHolder<Codec<? extends ActionTarget<?>>, Codec<NoneActionTarget>> NONE = register("none", NoneActionTarget.CODEC);

	public static <T extends ActionTarget<?>> DeferredHolder<Codec<? extends ActionTarget<?>>, Codec<T>> register(final String name, final Codec<T> codec) {
		return REGISTER.register(name, () -> codec);
	}

	public static void init(IEventBus modBus) {
		REGISTER.register(modBus);
	}
}
