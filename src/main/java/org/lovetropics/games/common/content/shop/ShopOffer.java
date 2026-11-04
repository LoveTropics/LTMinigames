package org.lovetropics.games.common.content.shop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStackTemplate;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.client_state.instance.ShopClientState;

import java.util.List;

/// Something players can buy in the [Shop]
///
/// @param cost     how many tropicoins it costs
/// @param target   who the offer is applied to, e.g. myself or another player
/// @param usableBy who can buy the offer
/// @param flavor   the intent of the offer - i.e. how it should be shown in UI and chat messages
/// @param actions  applied to the target
public record ShopOffer(
		ItemStackTemplate icon,
		Component name,
		List<Component> description,
		int cost,
		ShopTarget target,
		ShopUsers usableBy,
		ShopOfferFlavor flavor,
		GameActionList actions
) {
	public static final Codec<ShopOffer> CODEC = RecordCodecBuilder.create(in -> in.group(
			ItemStackTemplate.CODEC.fieldOf("icon").forGetter(ShopOffer::icon),
			ComponentSerialization.CODEC.fieldOf("name").forGetter(ShopOffer::name),
			ComponentSerialization.CODEC.listOf().optionalFieldOf("description", List.of()).forGetter(ShopOffer::description),
			ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("cost", 0).forGetter(ShopOffer::cost),
			ShopTarget.CODEC.optionalFieldOf("target", ShopTarget.SELF).forGetter(ShopOffer::target),
			ShopUsers.CODEC.optionalFieldOf("usable_by", ShopUsers.PLAYERS).forGetter(ShopOffer::usableBy),
			ShopOfferFlavor.CODEC.fieldOf("flavor").forGetter(ShopOffer::flavor),
			GameActionList.CODEC.fieldOf("actions").forGetter(ShopOffer::actions)
	).apply(in, ShopOffer::new));

	public ShopClientState.Offer asClientOffer() {
		return new ShopClientState.Offer(icon.create(), name, description, cost, target, flavor);
	}
}
