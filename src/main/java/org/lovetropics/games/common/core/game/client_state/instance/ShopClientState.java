package org.lovetropics.games.common.core.game.client_state.instance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.ItemStack;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;

import java.util.List;
import java.util.Optional;

/// @param balance how many tropicoins the player the shop is sent to has
public record ShopClientState(int balance, List<Offer> offers) implements GameClientState {
	public static final MapCodec<ShopClientState> CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
			Codec.INT.fieldOf("balance").forGetter(ShopClientState::balance),
			Offer.CODEC.listOf().fieldOf("offers").forGetter(ShopClientState::offers)
	).apply(in, ShopClientState::new));

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.SHOP.get();
	}

	/// @param cost how many tropicoins the offer costs
	public record Offer(ItemStack icon, Component name, Optional<Component> description, int cost) {
		public static final Codec<Offer> CODEC = RecordCodecBuilder.create(in -> in.group(
				// An icon that can't exist (e.g. more of an item than fits in a stack) ends up empty, which shouldn't kick players
				ItemStack.OPTIONAL_CODEC.fieldOf("icon").forGetter(Offer::icon),
				ComponentSerialization.CODEC.fieldOf("name").forGetter(Offer::name),
				ComponentSerialization.CODEC.optionalFieldOf("description").forGetter(Offer::description),
				Codec.INT.fieldOf("cost").forGetter(Offer::cost)
		).apply(in, Offer::new));
	}
}
