package com.lovetropics.minigames.common.util.registry;

import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.client_state.GameClientState;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehavior;
import com.lovetropics.minigames.common.core.game.predicate.entity.EntityPredicate;
import com.mojang.serialization.MapCodec;
import com.tterrag.registrate.AbstractRegistrate;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.fml.ModLoadingContext;

import java.util.function.Function;
import java.util.function.Supplier;

public final class LoveTropicsRegistrate extends AbstractRegistrate<LoveTropicsRegistrate> {
	private LoveTropicsRegistrate(String modid) {
		super(modid);
	}

	public static LoveTropicsRegistrate create(String modid) {
		return new LoveTropicsRegistrate(modid).registerEventListeners(ModLoadingContext.get().getActiveContainer().getEventBus());
	}

	public <T extends IGameBehavior> GameBehaviorBuilder<T, LoveTropicsRegistrate> behavior(MapCodec<T> codec) {
		return behavior(this, codec);
	}

	public <T extends IGameBehavior, P> GameBehaviorBuilder<T, P> behavior(P parent, MapCodec<T> codec) {
		return behavior(parent, currentName(), codec);
	}

	public <T extends IGameBehavior, P> GameBehaviorBuilder<T, P> behavior(P parent, String name, MapCodec<T> codec) {
		return entry(name, callback -> new GameBehaviorBuilder<>(this, parent, name, callback, codec));
	}

	public <T extends PersistentGameBehavior> PersistentGameBehaviorBuilder<T, LoveTropicsRegistrate> persistentBehavior(MapCodec<T> codec) {
		return persistentBehavior(this, codec);
	}

	public <T extends PersistentGameBehavior, P> PersistentGameBehaviorBuilder<T, P> persistentBehavior(P parent, MapCodec<T> codec) {
		return persistentBehavior(parent, currentName(), codec);
	}

	public <T extends PersistentGameBehavior, P> PersistentGameBehaviorBuilder<T, P> persistentBehavior(P parent, String name, MapCodec<T> codec) {
		return entry(name, callback -> new PersistentGameBehaviorBuilder<>(this, parent, name, callback, codec));
	}

	public <T extends EntityPredicate> EntityPredicateBuilder<T, LoveTropicsRegistrate> entityPredicate(MapCodec<T> codec) {
		return entityPredicate(this, currentName(), codec);
	}

	public <T extends EntityPredicate> EntityPredicateBuilder<T, LoveTropicsRegistrate> entityPredicate(String name, MapCodec<T> codec) {
		return entityPredicate(this, name, codec);
	}

	public <T extends EntityPredicate, P> EntityPredicateBuilder<T, P> entityPredicate(P parent, String name, MapCodec<T> codec) {
		return entry(name, callback -> new EntityPredicateBuilder<>(this, parent, name, callback, codec));
	}

	public <T extends LootItemConditionType> LootItemConditionTypeBuilder<T, LoveTropicsRegistrate> lootItemConditionType(Supplier<T> lootItemConditionTypeSupplier) {
		return lootItemConditionType(this, lootItemConditionTypeSupplier);
	}

	public <T extends LootItemConditionType, P> LootItemConditionTypeBuilder<T, P> lootItemConditionType(P parent, Supplier<T> lootItemConditionTypeSupplier) {
		return lootItemConditionType(parent, currentName(), lootItemConditionTypeSupplier);
	}

	public <T extends LootItemConditionType, P> LootItemConditionTypeBuilder<T, P> lootItemConditionType(P parent, String name, Supplier<T> lootItemConditionTypeSupplier) {
		return entry(name, callback -> new LootItemConditionTypeBuilder<>(this, parent, name, callback, lootItemConditionTypeSupplier));
	}

	public <T extends GameClientState> GameClientTweakBuilder<T, LoveTropicsRegistrate> clientState(MapCodec<T> codec) {
		return clientState(this, codec);
	}

	public <T extends GameClientState, P> GameClientTweakBuilder<T, P> clientState(P parent, MapCodec<T> codec) {
		return clientState(parent, currentName(), codec);
	}

	public <T extends GameClientState, P> GameClientTweakBuilder<T, P> clientState(P parent, String name, MapCodec<T> codec) {
		return entry(name, callback -> new GameClientTweakBuilder<>(this, parent, name, callback, codec));
	}

	public <T extends MobEffect> MobEffectBuilder<T, LoveTropicsRegistrate> mobEffect(Supplier<T> effect) {
		return mobEffect(this, effect);
	}

	public <T extends MobEffect, P> MobEffectBuilder<T, P> mobEffect(P parent, Supplier<T> effect) {
		return mobEffect(parent, currentName(), effect);
	}

	public <T extends MobEffect, P> MobEffectBuilder<T, P> mobEffect(P parent, String name, Supplier<T> effect) {
		return entry(name, callback -> new MobEffectBuilder<>(this, parent, name, callback, effect));
	}

	public <T extends Attribute> AttributeBuilder<T, LoveTropicsRegistrate> attribute(Function<String, T> attribute) {
		return attribute(this, attribute);
	}

	public <T extends Attribute, P> AttributeBuilder<T, P> attribute(P parent, Function<String, T> attribute) {
		return attribute(parent, currentName(), attribute);
	}

	public <T extends Attribute, P> AttributeBuilder<T, P> attribute(P parent, String name, Function<String, T> attribute) {
		return entry(name, callback -> new AttributeBuilder<>(this, parent, name, callback, attribute));
	}
}
