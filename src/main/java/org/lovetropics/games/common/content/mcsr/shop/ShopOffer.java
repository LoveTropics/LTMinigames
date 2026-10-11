package org.lovetropics.games.common.content.mcsr.shop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStackTemplate;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.client_state.instance.ShopClientState;

import java.util.Optional;

/// Something players can buy in the [Shop]
///
/// @param cost    how many tropicoins it costs
/// @param actions applied to the player who buys it
public record ShopOffer(ItemStackTemplate icon, Component name, Optional<Component> description, int cost, GameActionList actions) {
	public static final Codec<ShopOffer> CODEC = RecordCodecBuilder.create(in -> in.group(
			ItemStackTemplate.CODEC.fieldOf("icon").forGetter(ShopOffer::icon),
			ComponentSerialization.CODEC.fieldOf("name").forGetter(ShopOffer::name),
			ComponentSerialization.CODEC.optionalFieldOf("description").forGetter(ShopOffer::description),
			ExtraCodecs.NON_NEGATIVE_INT.fieldOf("cost").forGetter(ShopOffer::cost),
			GameActionList.CODEC.fieldOf("actions").forGetter(ShopOffer::actions)
	).apply(in, ShopOffer::new));

	public ShopClientState.Offer asClientOffer() {
		return new ShopClientState.Offer(icon.create(), name, description, cost);
	}
}
