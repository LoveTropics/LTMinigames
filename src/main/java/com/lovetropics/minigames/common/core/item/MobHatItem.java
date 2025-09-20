package com.lovetropics.minigames.common.core.item;

import com.lovetropics.minigames.common.core.diguise.DisguiseType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class MobHatItem extends Item {
	public MobHatItem(final Properties properties) {
		super(properties.equippable(EquipmentSlot.HEAD));
	}

	@Override
	public Component getName(final ItemStack stack) {
		final DisguiseType.EntityConfig entityType = stack.get(MinigameDataComponents.ENTITY);
		if (entityType != null) {
			return Component.translatable(getDescriptionId() + ".entity", entityType.type().getDescription());
		}
		return super.getName(stack);
	}
}
