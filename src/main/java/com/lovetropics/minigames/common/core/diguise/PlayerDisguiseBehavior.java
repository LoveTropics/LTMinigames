package com.lovetropics.minigames.common.core.diguise;

import com.lovetropics.minigames.LoveTropics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;
import java.util.Objects;

@EventBusSubscriber(modid = LoveTropics.ID)
public final class PlayerDisguiseBehavior {
	private static final ResourceLocation ATTRIBUTE_MODIFIER_ID = LoveTropics.location("disguise_modifier");

	@SubscribeEvent
	public static void onSetEntitySize(EntityEvent.Size event) {
		PlayerDisguise disguise = PlayerDisguise.getOrNull(event.getEntity());
		if (disguise == null || !disguise.isDisguised()) {
			return;
		}

		Entity entity = Objects.requireNonNullElse(disguise.entity(), event.getEntity());
		float scale = disguise.type().scale();

		Pose pose = event.getPose();
		EntityDimensions disguiseDimensions = entity.getDimensions(pose);
		EntityDimensions actualDimensions;
		if (disguise.type().changesSize()) {
			actualDimensions = disguiseDimensions;
		} else {
			actualDimensions = event.getEntity().getDimensions(pose).withEyeHeight(disguiseDimensions.eyeHeight());
		}
		event.setNewSize(actualDimensions.scale(scale));
	}

	public static void applyAttributes(LivingEntity entity, LivingEntity disguise) {
		AttributeMap playerAttributes = entity.getAttributes();
		AttributeMap disguiseAttributes = disguise.getAttributes();

		BuiltInRegistries.ATTRIBUTE.listElements().forEach(attribute -> {
			if (!disguiseAttributes.hasAttribute(attribute) || !playerAttributes.hasAttribute(attribute)) {
				return;
			}

			AttributeInstance playerInstance = playerAttributes.getInstance(attribute);
			AttributeInstance disguiseInstance = disguiseAttributes.getInstance(attribute);

			AttributeModifier modifier = createModifier(playerInstance, disguiseInstance);
			if (modifier != null) {
				playerInstance.addTransientModifier(modifier);
			}
		});
	}

	public static void clearAttributes(LivingEntity entity) {
		AttributeMap attributes = entity.getAttributes();
		BuiltInRegistries.ATTRIBUTE.listElements().forEach(attribute -> {
			AttributeInstance instance = attributes.getInstance(attribute);
			if (instance != null) {
				instance.removeModifier(ATTRIBUTE_MODIFIER_ID);
			}
		});
	}

	@Nullable
	private static AttributeModifier createModifier(AttributeInstance player, AttributeInstance disguise) {
		double playerValue = player.getBaseValue();
		double disguiseValue = disguise.getBaseValue();

		// non-player movement speeds get effectively squared
		if (disguise.getAttribute() == Attributes.MOVEMENT_SPEED) {
			disguiseValue *= disguiseValue;
		}

		double value = disguiseValue - playerValue;
		if (Math.abs(value) <= 0.001) {
			return null;
		}

		return new AttributeModifier(
				ATTRIBUTE_MODIFIER_ID,
				value,
				AttributeModifier.Operation.ADD_VALUE
		);
	}

	public static void copyWalkAnimation(WalkAnimationState from, WalkAnimationState to) {
		to.update(from.position() - to.position() - from.speed(), 1.0f, 1.0f);
		to.setSpeed(from.speed(0.0f));
		to.update(from.speed(), 1.0f, 1.0f);
	}

	public static void onDisguiseChange(LivingEntity entity) {
		if (entity instanceof Player player) {
			player.refreshDisplayName();
			if (player instanceof ServerPlayer serverPlayer) {
				serverPlayer.refreshTabListName();
			}
		}
	}

	@SubscribeEvent
	public static void onPlayerNameFormat(PlayerEvent.NameFormat event) {
		event.setDisplayname(updateDisplayName(event.getEntity(), event.getDisplayname()));
	}

	@SubscribeEvent
	public static void onPlayerTabListNameFormat(PlayerEvent.TabListNameFormat event) {
		event.setDisplayName(updateDisplayName(event.getEntity(), event.getDisplayName()));
	}

	@Contract("_,!null->!null")
	@Nullable
	private static Component updateDisplayName(Player player, @Nullable Component name) {
		PlayerDisguise disguise = PlayerDisguise.getOrNull(player);
		if (disguise != null) {
			Component customName = disguise.type().customName();
			if (customName != null) {
				return customName;
			}
		}
		return name;
	}
}
