package com.lovetropics.minigames.client.toast;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStackTemplate;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public final class NotificationIcon {
	public static final Codec<NotificationIcon> CODEC = RecordCodecBuilder.create(i -> i.group(
			ItemStackTemplate.CODEC.optionalFieldOf("item").forGetter(c -> Optional.ofNullable(c.item)),
			MobEffect.CODEC.optionalFieldOf("effect").forGetter(c -> Optional.ofNullable(c.effect))
	).apply(i, NotificationIcon::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, NotificationIcon> STREAM_CODEC = ByteBufCodecs.either(ItemStackTemplate.STREAM_CODEC, MobEffect.STREAM_CODEC).map(
			either -> either.map(NotificationIcon::item, NotificationIcon::effect),
			icon -> {
				if (icon.item != null) {
					return Either.left(icon.item);
				} else {
					return Either.right(icon.effect);
				}
			}
	);

	public final @Nullable ItemStackTemplate item;
	public final @Nullable Holder<MobEffect> effect;

	private NotificationIcon(Optional<ItemStackTemplate> item, Optional<Holder<MobEffect>> effect) {
		this(item.orElse(null), effect.orElse(null));
	}

	private NotificationIcon(@Nullable ItemStackTemplate item, @Nullable Holder<MobEffect> effect) {
		this.item = item;
		this.effect = effect;
	}

	public static NotificationIcon item(ItemStackTemplate item) {
		return new NotificationIcon(item, null);
	}

	public static NotificationIcon effect(Holder<MobEffect> effect) {
		return new NotificationIcon(null, effect);
	}
}
