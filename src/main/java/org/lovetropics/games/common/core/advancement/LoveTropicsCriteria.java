package org.lovetropics.games.common.core.advancement;

import com.mojang.serialization.Codec;
import net.minecraft.advancements.predicates.entity.EntitySubPredicate;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lovetropics.games.LoveTropics;

/// Advancement triggers and predicates for what vanilla can't express
public final class LoveTropicsCriteria {
	private static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, LoveTropics.ID);
	private static final DeferredRegister<DataComponentPredicate.Type<?>> ITEM_PREDICATES = DeferredRegister.create(Registries.DATA_COMPONENT_PREDICATE_TYPE, LoveTropics.ID);
	private static final DeferredRegister<Codec<? extends EntitySubPredicate>> ENTITY_PREDICATES = DeferredRegister.create(Registries.ENTITY_SUB_PREDICATE_TYPE, LoveTropics.ID);

	public static final DeferredHolder<CriterionTrigger<?>, RaidWaveCompletedTrigger> RAID_WAVE_COMPLETED = TRIGGERS.register("raid_wave_completed", RaidWaveCompletedTrigger::new);

	static {
		ITEM_PREDICATES.register("banner_patterns", () -> new DataComponentPredicate.ConcreteType<>(BannerPatternsPredicate.CODEC));
		ENTITY_PREDICATES.register("distinct_armor_trims", () -> DistinctArmorTrimsPredicate.CODEC);
	}

	public static void init(IEventBus modBus) {
		TRIGGERS.register(modBus);
		ITEM_PREDICATES.register(modBus);
		ENTITY_PREDICATES.register(modBus);
	}
}
