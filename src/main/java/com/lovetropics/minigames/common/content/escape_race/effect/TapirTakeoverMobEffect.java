package com.lovetropics.minigames.common.content.escape_race.effect;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jspecify.annotations.Nullable;
import org.lovetropics.peekaboo.api.Disguise;
import org.lovetropics.peekaboo.api.EntityDisguiseHolder;

@EventBusSubscriber(modid = LoveTropics.ID)
public class TapirTakeoverMobEffect extends MobEffect {
	private static final DeferredHolder<EntityType<?>, EntityType<?>> TAPIR = DeferredHolder.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("tropicraft", "tapir"));

	public TapirTakeoverMobEffect(MobEffectCategory category) {
		super(category, CommonColors.WHITE);
	}

	@Override
	public void onEffectStarted(LivingEntity entity, int amplifier) {
		EntityDisguiseHolder disguiseHolder = EntityDisguiseHolder.getOrNull(entity);

		if (TAPIR.isBound() && disguiseHolder != null) {
			Disguise disguise = Disguise.of(TAPIR.get());
			disguiseHolder.set(disguise);
		}
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@SubscribeEvent
	public static void onEffectExpired(MobEffectEvent.Expired event) {
		removeDisguise(event.getEffectInstance(), event.getEntity());
	}

	@SubscribeEvent
	public static void onEffectRemoved(MobEffectEvent.Remove event) {
		removeDisguise(event.getEffectInstance(), event.getEntity());
	}

	private static void removeDisguise(@Nullable MobEffectInstance effectInstance, LivingEntity livingEntity) {
		if (effectInstance != null && effectInstance.is(EscapeRace.TAPIR_TAKEOVER)) {
			EntityDisguiseHolder disguiseHolder = EntityDisguiseHolder.getOrNull(livingEntity);
			if (disguiseHolder != null && disguiseHolder.disguise().equals(Disguise.of(TAPIR.get()))) {
				disguiseHolder.clear();
			}
		}
	}
}
