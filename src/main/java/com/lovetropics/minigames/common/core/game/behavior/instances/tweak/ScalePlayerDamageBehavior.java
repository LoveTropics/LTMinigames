package com.lovetropics.minigames.common.core.game.behavior.instances.tweak;

import com.lovetropics.lib.permission.PermissionsApi;
import com.lovetropics.lib.permission.role.RoleOverrideReader;
import com.lovetropics.lib.permission.role.RoleOverrideType;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorTypes;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.role.StreamHosts;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public record ScalePlayerDamageBehavior(float factor, Map<RoleOverrideType<?>, RoleOverrideEntry<?>> roleFactors) implements IGameBehavior {

	public static final Codec<Map<RoleOverrideType<?>, RoleOverrideEntry<?>>> OVERRIDE_ENTRY = Codec.dispatchedMap(
			RoleOverrideType.REGISTRY, RoleOverrideEntry::create
	);

	public static final MapCodec<ScalePlayerDamageBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.FLOAT.fieldOf("factor").forGetter(ScalePlayerDamageBehavior::factor),
			OVERRIDE_ENTRY.optionalFieldOf("role_factors", Map.of()).forGetter(ScalePlayerDamageBehavior::roleFactors)
	).apply(i, ScalePlayerDamageBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GamePlayerEvents.DAMAGE_AMOUNT, (player, damageSource, amount, originalAmount) -> {
			if (amount <= 1.0f) {
				return amount;
			}
			float factor = this.factor;
			RoleOverrideReader overrides = PermissionsApi.lookup().byEntity(player).overrides();
			for (RoleOverrideEntry<?> entry : roleFactors.values()) {
				if (entry.test(overrides)) {
					factor = entry.factor();
				}
			}

			float newAmount = amount * factor;
			if (StreamHosts.isHost(player) && newAmount >= player.getMaxHealth() / 2.0f && newAmount <= player.getMaxHealth() * 2.0f) {
				newAmount = Math.min(player.getHealth() - 1.0f, newAmount);
			}
			return Math.max(1.0f, newAmount);
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return GameBehaviorTypes.SCALE_PLAYER_DAMAGE;
	}

	public record RoleOverrideEntry<T>(RoleOverrideType<T> type, T value, float factor) {
		public static Codec<RoleOverrideEntry<?>> create(RoleOverrideType<?> type) {
			return (Codec<RoleOverrideEntry<?>>) (Object) codec(type);
		}

		public static <T> Codec<RoleOverrideEntry<T>> codec(RoleOverrideType<T> type) {
			return codec(type, type.getCodec());
		}

		public static <T> Codec<RoleOverrideEntry<T>> codec(RoleOverrideType<T> type, Codec<T> valueCodec) {
			return RecordCodecBuilder.create(in -> in.group(
					MapCodec.unit(type).forGetter(RoleOverrideEntry::type),
					valueCodec.fieldOf("value").forGetter(RoleOverrideEntry::value),
					Codec.FLOAT.fieldOf("factor").forGetter(RoleOverrideEntry::factor)
			).apply(in, RoleOverrideEntry::new));
		}

		public boolean test(RoleOverrideReader overrides) {
			return Objects.equals(overrides.getOrNull(type), value);
		}
	}
}
