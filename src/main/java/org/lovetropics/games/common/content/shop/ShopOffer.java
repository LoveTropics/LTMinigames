package org.lovetropics.games.common.content.shop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStackTemplate;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.client_state.instance.ShopClientState;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/// Something players can buy in the [Shop]
///
/// @param id             id for reference by commands
/// @param cost           how many tropicoins it costs
/// @param stock          how many times each player can buy it, or unlimited if empty - overlords are never limited
/// @param target         who the offer is applied to, e.g. myself or another player
/// @param includeSelf    true if the self player should also be included in the action, alongside any other [#target()]
/// @param targetCooldown for offers used on other players, how many seconds it takes before it can be used on the same player again - overlords ignore it
/// @param usableBy       who can buy the offer, e.g. players and ghosts but not overlords
/// @param flavor         the intent of the offer - i.e. how it should be shown in UI and chat messages
/// @param actions        applied to the target
public record ShopOffer(
		String id,
		ItemStackTemplate icon,
		Component name,
		List<Component> description,
		int cost,
		Optional<Integer> stock,
		ShopTarget target,
		boolean targetTeam,
		boolean includeSelf,
		Optional<Integer> targetCooldown,
		List<ShopUsers> usableBy,
		ShopOfferFlavor flavor,
		GameActionList actions
) {
	public static final Codec<ShopOffer> CODEC = RecordCodecBuilder.create(in -> in.group(
			Codec.STRING.fieldOf("id").forGetter(ShopOffer::id),
			ItemStackTemplate.CODEC.fieldOf("icon").forGetter(ShopOffer::icon),
			ComponentSerialization.CODEC.fieldOf("name").forGetter(ShopOffer::name),
			ComponentSerialization.CODEC.listOf().optionalFieldOf("description", List.of()).forGetter(ShopOffer::description),
			ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("cost", 0).forGetter(ShopOffer::cost),
			ExtraCodecs.POSITIVE_INT.optionalFieldOf("stock").forGetter(ShopOffer::stock),
			ShopTarget.CODEC.optionalFieldOf("target", ShopTarget.SELF).forGetter(ShopOffer::target),
			Codec.BOOL.optionalFieldOf("target_team", false).forGetter(ShopOffer::targetTeam),
			Codec.BOOL.optionalFieldOf("include_self", false).forGetter(ShopOffer::includeSelf),
			ExtraCodecs.POSITIVE_INT.optionalFieldOf("target_cooldown").forGetter(ShopOffer::targetCooldown),
			ExtraCodecs.compactListCodec(ShopUsers.CODEC).optionalFieldOf("usable_by", List.of(ShopUsers.PLAYERS)).forGetter(ShopOffer::usableBy),
			ShopOfferFlavor.CODEC.fieldOf("flavor").forGetter(ShopOffer::flavor),
			GameActionList.CODEC.fieldOf("actions").forGetter(ShopOffer::actions)
	).apply(in, ShopOffer::new));

	/// @return how many ticks it takes before the offer can be used on the same player again if it has a cooldown
	public Optional<Integer> targetCooldownTicks() {
		return target.requiresOtherPlayer() ? targetCooldown.map(seconds -> seconds * SharedConstants.TICKS_PER_SECOND) : Optional.empty();
	}

	public boolean isUsableByPlayers() {
		return usableBy.stream().anyMatch(ShopUsers::includesPlayers);
	}

	public boolean isUsableByGhosts() {
		return usableBy.stream().anyMatch(ShopUsers::includesGhosts);
	}

	public boolean isUsableByOverlords() {
		return usableBy.stream().anyMatch(ShopUsers::includesOverlords);
	}

	public ShopOffer forOverlords() {
		ShopTarget overlordTarget = target == ShopTarget.RANDOM ? ShopTarget.PICK : target;
		// no cost no cooldown
		return new ShopOffer(id, icon, name, description, 0, Optional.empty(), overlordTarget, targetTeam, includeSelf, Optional.empty(), usableBy, flavor, actions);
	}

	public ShopOffer withCost(int cost) {
		return new ShopOffer(id, icon, name, description, cost, stock, target, targetTeam, includeSelf, targetCooldown, usableBy, flavor, actions);
	}

	public ShopOffer withStock(Optional<Integer> stock) {
		return new ShopOffer(id, icon, name, description, cost, stock, target, targetTeam, includeSelf, targetCooldown, usableBy, flavor, actions);
	}

	/// @param remainingStock how many more times the player the shop is sent to can buy it, if limited
	/// @param cooldowns      how many ticks are left before the offer can be used on each player or team again
	public ShopClientState.Offer asClientOffer(Optional<Integer> remainingStock, Map<UUID, Integer> cooldowns) {
		return new ShopClientState.Offer(icon.create(), name, description, cost, remainingStock, targetCooldownTicks(), cooldowns, target, targetTeam, flavor);
	}
}
