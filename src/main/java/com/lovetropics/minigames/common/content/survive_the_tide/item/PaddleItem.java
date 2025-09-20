package com.lovetropics.minigames.common.content.survive_the_tide.item;

import com.lovetropics.minigames.common.content.survive_the_tide.entity.DriftwoodEntity;
import com.lovetropics.minigames.common.content.survive_the_tide.entity.DriftwoodRider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class PaddleItem extends Item {
	public PaddleItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level world, Player player, InteractionHand hand) {
		if (world.isClientSide) {
			return InteractionResult.PASS;
		}

		DriftwoodRider rider = player.getData(DriftwoodRider.ATTACHMENT);
		DriftwoodEntity driftwood = rider.getRiding();
		if (driftwood != null) {
			if (driftwood.paddle(player.getYRot())) {
				player.swing(hand, true);
				return InteractionResult.SUCCESS;
			} else {
				return InteractionResult.FAIL;
			}
		}

		return InteractionResult.PASS;
	}
}
