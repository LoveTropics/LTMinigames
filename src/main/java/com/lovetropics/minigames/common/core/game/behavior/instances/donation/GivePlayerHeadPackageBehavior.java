package com.lovetropics.minigames.common.core.game.behavior.instances.donation;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContextKeys;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.util.Util;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.Optional;

public record GivePlayerHeadPackageBehavior(boolean forced) implements IGameBehavior {
	public static final MapCodec<GivePlayerHeadPackageBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.BOOL.optionalFieldOf("forced", false).forGetter(c -> c.forced)
	).apply(i, GivePlayerHeadPackageBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.applyToPlayers(game, (context, player) -> {
			String sendingPlayer = context.getOrDefault(GameActionContextKeys.PACKAGE_SENDER, "LoveTropics");

			ItemStack head = createHeadForSender(sendingPlayer);
			if (forced) {
				head.enchant(game.registryAccess().holderOrThrow(Enchantments.BINDING_CURSE), 1);
				player.setItemSlot(EquipmentSlot.HEAD, head);
			} else {
				Util.addItemStackToInventory(player, head);
			}

			return true;
		});
	}

	private ItemStack createHeadForSender(String sendingPlayer) {
		final ItemStack senderHead = new ItemStack(Items.PLAYER_HEAD);
		if (StringUtil.isValidPlayerName(sendingPlayer)) {
			senderHead.set(DataComponents.PROFILE, new ResolvableProfile(Optional.of(sendingPlayer), Optional.empty(), new PropertyMap()));
		}
		return senderHead;
	}
}
