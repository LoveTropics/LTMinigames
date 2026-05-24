package com.lovetropics.minigames.common.content.qottott;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.qottott.behavior.CoinDropAttributeBehavior;
import com.lovetropics.minigames.common.content.qottott.behavior.GivePointsAction;
import com.lovetropics.minigames.common.content.qottott.behavior.ItemDropperBehavior;
import com.lovetropics.minigames.common.content.qottott.behavior.ItemPickupPriorityBehavior;
import com.lovetropics.minigames.common.content.qottott.behavior.KitSelectionBehavior;
import com.lovetropics.minigames.common.content.qottott.behavior.LeakyPocketsBehavior;
import com.lovetropics.minigames.common.content.qottott.behavior.LobbyWithPortalBehavior;
import com.lovetropics.minigames.common.content.qottott.behavior.PowerUpIndicatorBehavior;
import com.lovetropics.minigames.common.util.registry.GameBehaviorEntry;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;
import net.minecraft.core.Holder;
import net.minecraft.util.CommonColors;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

@EventBusSubscriber(modid = LoveTropics.ID)
public class Qottott {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameBehaviorEntry<ItemDropperBehavior> ITEM_DROPPER = REGISTRATE.object("item_dropper").behavior(ItemDropperBehavior.CODEC).register();
	public static final GameBehaviorEntry<GivePointsAction> GIVE_POINTS = REGISTRATE.object("give_points").behavior(GivePointsAction.CODEC).register();
	public static final GameBehaviorEntry<LobbyWithPortalBehavior> LOBBY_WITH_PORTAL = REGISTRATE.object("lobby_with_portal").behavior(LobbyWithPortalBehavior.CODEC).register();
	public static final GameBehaviorEntry<KitSelectionBehavior> KIT_SELECTION = REGISTRATE.object("kit_selection").behavior(KitSelectionBehavior.CODEC).register();
	public static final GameBehaviorEntry<ItemPickupPriorityBehavior> PICKUP_PRIORITY_BEHAVIOR = REGISTRATE.object("pickup_priority").behavior(ItemPickupPriorityBehavior.CODEC).register();
	public static final GameBehaviorEntry<CoinDropAttributeBehavior> COIN_DROP_ATTRIBUTE_BEHAVIOR = REGISTRATE.object("coin_drop_attribute").behavior(CoinDropAttributeBehavior.CODEC).register();
	public static final GameBehaviorEntry<PowerUpIndicatorBehavior> POWER_UP_INDICATOR = REGISTRATE.object("power_up_indicator").behavior(PowerUpIndicatorBehavior.CODEC).register();
	public static final GameBehaviorEntry<LeakyPocketsBehavior> LEAKY_POCKETS_BEHAVIOR = REGISTRATE.object("leaky_pockets").behavior(LeakyPocketsBehavior.CODEC).register();

	public static final Holder<Attribute> COIN_MULTIPLIER = REGISTRATE.object("coin_multiplier").attribute(translationKey ->
			new RangedAttribute(translationKey, 1.0, 0.0, 100.0).setSyncable(false)
	).lang("Coin Multiplier").register();
	public static final Holder<Attribute> PICKUP_PRIORITY = REGISTRATE.object("pickup_priority").attribute(translationKey ->
			new RangedAttribute(translationKey, 0.0, -100.0, 100.0).setSyncable(false)
	).lang("Item Pickup Priority").register();
	public static final Holder<Attribute> COIN_DROPS = REGISTRATE.object("coin_drops").attribute(translationKey ->
			new RangedAttribute(translationKey, 0.0, 0.0, 1.0).setSyncable(false)
	).lang("Coin Drops").register();
	public static final Holder<Attribute> LEAKY_POCKETS = REGISTRATE.object("leaky_pockets").attribute(translationKey ->
			new RangedAttribute(translationKey, 0.0, 0.0, 1.0).setSyncable(false)
	).lang("Leaky Pockets").register();

	public static final Holder<MobEffect> COIN_MULTIPLIER_POWER_UP = REGISTRATE.object("coin_multiplier_power_up").mobEffect(() -> new CustomMobEffect(MobEffectCategory.BENEFICIAL).addAttributeModifier(
			COIN_MULTIPLIER,
			LoveTropics.id("coin_multiplier_power_up_effect"),
			1.0,
			AttributeModifier.Operation.ADD_MULTIPLIED_BASE
	)).lang("Coin Multiplier Power-up").register();
	public static final Holder<MobEffect> PICKUP_PRIORITY_POWER_UP = REGISTRATE.object("pickup_priority_power_up").mobEffect(() -> new CustomMobEffect(MobEffectCategory.BENEFICIAL).addAttributeModifier(
			PICKUP_PRIORITY,
			LoveTropics.id("pickup_priority_power_up_effect"),
			1.0,
			AttributeModifier.Operation.ADD_VALUE
	)).lang("Pickup Priority Power-up").register();
	public static final Holder<MobEffect> KNOCKBACK_RESISTANCE_POWER_UP = REGISTRATE.object("knockback_resistance_power_up").mobEffect(() -> new CustomMobEffect(MobEffectCategory.BENEFICIAL).addAttributeModifier(
			Attributes.KNOCKBACK_RESISTANCE,
			LoveTropics.id("knockback_resistance_power_up_effect"),
			1.0,
			AttributeModifier.Operation.ADD_VALUE
	)).lang("Knockback Resistance Power-Up").register();
	public static final Holder<MobEffect> SPEED_POWER_UP = REGISTRATE.object("speed_power_up").mobEffect(() -> new CustomMobEffect(MobEffectCategory.BENEFICIAL).addAttributeModifier(
			Attributes.MOVEMENT_SPEED,
			LoveTropics.id("speed_power_up_effect"),
			0.05,
			AttributeModifier.Operation.ADD_VALUE
	)).lang("Speed Power-up").register();

	public static final Holder<MobEffect> LEAKY_POCKETS_EFFECT = REGISTRATE.object("leaky_pockets").mobEffect(() -> new CustomMobEffect(MobEffectCategory.HARMFUL).addAttributeModifier(
			LEAKY_POCKETS,
			LoveTropics.id("leaky_pockets_effect"),
			0.005,
			AttributeModifier.Operation.ADD_VALUE
	)).lang("Leaky Pockets").register();

	public static void init() {
	}

	@SubscribeEvent
	public static void onModifyDefaultAttributes(final EntityAttributeModificationEvent event) {
		event.add(EntityType.PLAYER, COIN_MULTIPLIER);
		event.add(EntityType.PLAYER, PICKUP_PRIORITY);
		event.add(EntityType.PLAYER, COIN_DROPS);
		event.add(EntityType.PLAYER, LEAKY_POCKETS);
	}

	private static class CustomMobEffect extends MobEffect {
		protected CustomMobEffect(final MobEffectCategory category) {
			super(category, CommonColors.WHITE);
		}
	}
}
