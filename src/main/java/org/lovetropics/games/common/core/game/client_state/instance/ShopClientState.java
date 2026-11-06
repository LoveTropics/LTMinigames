package org.lovetropics.games.common.core.game.client_state.instance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import org.lovetropics.games.common.content.shop.ShopOfferFlavor;
import org.lovetropics.games.common.content.shop.ShopTarget;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/// @param balance how many tropicoins the player the shop is sent to has, if applicable
/// @param playerTargets the other players that offers can be used on, with the most points first
public record ShopClientState(
		Optional<Integer> balance,
		List<Offer> offers,
		List<Target> playerTargets,
		List<Target> teamTargets,
		List<Sabotage> sabotages
) implements GameClientState {
	public static final MapCodec<ShopClientState> CODEC = RecordCodecBuilder.mapCodec(in -> in.group(
			Codec.INT.optionalFieldOf("balance").forGetter(ShopClientState::balance),
			Offer.CODEC.listOf().fieldOf("offers").forGetter(ShopClientState::offers),
			Target.CODEC.listOf().optionalFieldOf("player_targets", List.of()).forGetter(ShopClientState::playerTargets),
			Target.CODEC.listOf().optionalFieldOf("team_targets", List.of()).forGetter(ShopClientState::teamTargets),
			Sabotage.CODEC.listOf().optionalFieldOf("sabotages", List.of()).forGetter(ShopClientState::sabotages)
	).apply(in, ShopClientState::new));

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.SHOP.get();
	}

	/// @param cost           how many tropicoins the offer costs
	/// @param stock          how many more times the player can buy the offer, if limited
	/// @param targetCooldown how many ticks it takes before the offer can be used on the same player or team again, if it has a cooldown
	/// @param cooldowns      the players or teams it can't be used on yet, with how many ticks were left when the shop was sent
	/// @param target         who the offer is used on
	/// @param targetTeam     true if the offer is used on teams rather than players
	public record Offer(
			ItemStack icon,
			Component name,
			List<Component> description,
			int cost,
			Optional<Integer> stock,
			Optional<Integer> targetCooldown,
			Map<UUID, Integer> cooldowns,
			ShopTarget target,
			boolean targetTeam,
			ShopOfferFlavor flavor
	) {
		public static final Codec<Offer> CODEC = RecordCodecBuilder.create(in -> in.group(
				// An icon that can't exist (e.g. more of an item than fits in a stack) ends up empty, which shouldn't kick players
				ItemStack.OPTIONAL_CODEC.fieldOf("icon").forGetter(Offer::icon),
				ComponentSerialization.CODEC.fieldOf("name").forGetter(Offer::name),
				ExtraCodecs.compactListCodec(ComponentSerialization.CODEC).optionalFieldOf("description", List.of()).forGetter(Offer::description),
				Codec.INT.fieldOf("cost").forGetter(Offer::cost),
				Codec.INT.optionalFieldOf("stock").forGetter(Offer::stock),
				Codec.INT.optionalFieldOf("target_cooldown").forGetter(Offer::targetCooldown),
				Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT).optionalFieldOf("cooldowns", Map.of()).forGetter(Offer::cooldowns),
				ShopTarget.CODEC.optionalFieldOf("target", ShopTarget.SELF).forGetter(Offer::target),
				Codec.BOOL.optionalFieldOf("target_team", false).forGetter(Offer::targetTeam),
				ShopOfferFlavor.CODEC.fieldOf("flavor").forGetter(Offer::flavor)
		).apply(in, Offer::new));

		public boolean isSoldOut() {
			return stock.isPresent() && stock.get() <= 0; // || isBlackFriday() ?
		}

		public List<Target> targetsIn(ShopClientState shop) {
			return targetTeam ? shop.teamTargets : shop.playerTargets;
		}

		/// The shop is sent again once the cooldown is over
		public boolean isOnCooldown(UUID target) {
			return cooldowns.containsKey(target);
		}
	}

	public record Target(
			UUID id,
			Component name,
			int points
	) {
		public static final Codec<Target> CODEC = RecordCodecBuilder.create(in -> in.group(
				UUIDUtil.CODEC.fieldOf("id").forGetter(Target::id),
				ComponentSerialization.CODEC.fieldOf("name").forGetter(Target::name),
				Codec.INT.fieldOf("points").forGetter(Target::points)
		).apply(in, Target::new));
	}

	public record Sabotage(int ticks, Component buyer, Component target, Component offer) {
		public static final Codec<Sabotage> CODEC = RecordCodecBuilder.create(in -> in.group(
				Codec.INT.fieldOf("ticks").forGetter(Sabotage::ticks),
				ComponentSerialization.CODEC.fieldOf("buyer").forGetter(Sabotage::buyer),
				ComponentSerialization.CODEC.fieldOf("target").forGetter(Sabotage::target),
				ComponentSerialization.CODEC.fieldOf("offer").forGetter(Sabotage::offer)
		).apply(in, Sabotage::new));
	}
}
