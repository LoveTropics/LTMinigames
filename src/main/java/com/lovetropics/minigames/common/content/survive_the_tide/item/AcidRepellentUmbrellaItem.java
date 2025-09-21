package com.lovetropics.minigames.common.content.survive_the_tide.item;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.survive_the_tide.SurviveTheTide;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = LoveTropics.ID)
public class AcidRepellentUmbrellaItem extends Item {
	public AcidRepellentUmbrellaItem(Properties properties) {
		super(properties.durability(180));
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (player.getDeltaMovement().y() < 0.0 && !player.getAbilities().flying && isHoldingItem(player, SurviveTheTide.ACID_REPELLENT_UMBRELLA.get())) {
			player.setDeltaMovement(player.getDeltaMovement().multiply(1.0, 0.8, 1.0));
			player.fallDistance = 0.0F;
		}
	}

	private static boolean isHoldingItem(Player player, Item item) {
		return player.getMainHandItem().getItem() == item || player.getOffhandItem().getItem() == item;
	}
}
