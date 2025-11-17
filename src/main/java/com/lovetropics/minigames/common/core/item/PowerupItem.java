package com.lovetropics.minigames.common.core.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

public class PowerupItem extends Item {

	public PowerupItem(Properties properties) {
		super(properties);
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 1;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		player.swing(hand);
		return super.use(level, player, hand);
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.TOOT_HORN;
	}
}
