package org.lovetropics.games.client.toast;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStackTemplate;
import org.jspecify.annotations.Nullable;

public final class NotificationIcon {
	public static final Codec<NotificationIcon> CODEC = Codec.either(
			ItemStackTemplate.CODEC.fieldOf("item").codec(),
			MobEffect.CODEC.fieldOf("effect").codec()
	).xmap(NotificationIcon::eitherToIcon, NotificationIcon::iconToEither);

	public static final StreamCodec<RegistryFriendlyByteBuf, NotificationIcon> STREAM_CODEC = ByteBufCodecs.either(ItemStackTemplate.STREAM_CODEC, MobEffect.STREAM_CODEC)
			.map(NotificationIcon::eitherToIcon, NotificationIcon::iconToEither);

	private static NotificationIcon eitherToIcon(Either<ItemStackTemplate, Holder<MobEffect>> either) {
		return either.map(NotificationIcon::item, NotificationIcon::effect);
	}

	private static Either<ItemStackTemplate, Holder<MobEffect>> iconToEither(NotificationIcon icon) {
		if (icon.item != null) {
			return Either.left(icon.item);
		} else {
			return Either.right(icon.effect);
		}
	}

	public final @Nullable ItemStackTemplate item;
	public final @Nullable Holder<MobEffect> effect;

	private NotificationIcon(@Nullable ItemStackTemplate item, @Nullable Holder<MobEffect> effect) {
		if (item == null && effect == null) {
			throw new IllegalArgumentException("One of item or effect must be set");
		}
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
