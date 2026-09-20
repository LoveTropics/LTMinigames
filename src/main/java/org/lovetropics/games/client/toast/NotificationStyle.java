package org.lovetropics.games.client.toast;

import com.lovetropics.lib.codec.MoreCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;

public record NotificationStyle(NotificationIcon icon, Sentiment sentiment, Color color, long visibleTimeMs) {
	public static final MapCodec<NotificationStyle> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			NotificationIcon.CODEC.fieldOf("icon").forGetter(NotificationStyle::icon),
			Sentiment.CODEC.optionalFieldOf("sentiment", Sentiment.NEUTRAL).forGetter(NotificationStyle::sentiment),
			Color.CODEC.optionalFieldOf("color", Color.LIGHT).forGetter(NotificationStyle::color),
			Codec.LONG.optionalFieldOf("visible_time_ms", 5 * 1000L).forGetter(NotificationStyle::visibleTimeMs)
	).apply(i, NotificationStyle::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, NotificationStyle> STREAM_CODEC = StreamCodec.composite(
			NotificationIcon.STREAM_CODEC, NotificationStyle::icon,
			Sentiment.STREAM_CODEC, NotificationStyle::sentiment,
			Color.STREAM_CODEC, NotificationStyle::color,
			ByteBufCodecs.VAR_LONG, NotificationStyle::visibleTimeMs,
			NotificationStyle::new
	);

	public enum Color {
		DARK("dark"),
		LIGHT("light");

		public static final Color[] VALUES = values();
		public static final Codec<Color> CODEC = MoreCodecs.stringVariants(VALUES, c -> c.name);
		public static final StreamCodec<ByteBuf, Color> STREAM_CODEC = ByteBufCodecs.idMapper(
				ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO),
				Enum::ordinal
		);

		private final String name;

		Color(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	public enum Sentiment {
		NEUTRAL("neutral"),
		POSITIVE("positive"),
		NEGATIVE("negative");

		public static final Sentiment[] VALUES = values();
		public static final Codec<Sentiment> CODEC = MoreCodecs.stringVariants(VALUES, s -> s.name);
		public static final StreamCodec<ByteBuf, Sentiment> STREAM_CODEC = ByteBufCodecs.idMapper(
				ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO),
				Enum::ordinal
		);

		public final String name;

		Sentiment(String name) {
			this.name = name;
		}

		public ChatFormatting textStyle() {
			return switch (this) {
				case NEUTRAL -> ChatFormatting.YELLOW;
				case POSITIVE -> ChatFormatting.GREEN;
				case NEGATIVE -> ChatFormatting.RED;
			};
		}

		public String getName() {
			return name;
		}
	}
}
