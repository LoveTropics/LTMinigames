package com.lovetropics.minigames.client.toast;

import com.lovetropics.lib.codec.MoreCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;

public record NotificationStyle(NotificationIcon icon, Sentiment sentiment, Color color, long visibleTimeMs) {
	public static final MapCodec<NotificationStyle> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			NotificationIcon.CODEC.fieldOf("icon").forGetter(NotificationStyle::icon),
			Sentiment.CODEC.optionalFieldOf("sentiment", Sentiment.NEUTRAL).forGetter(NotificationStyle::sentiment),
			Color.CODEC.optionalFieldOf("color", Color.LIGHT).forGetter(NotificationStyle::color),
			Codec.LONG.optionalFieldOf("visible_time_ms", 5 * 1000L).forGetter(NotificationStyle::visibleTimeMs)
	).apply(i, NotificationStyle::new));

	public void encode(FriendlyByteBuf buffer) {
		this.icon.encode(buffer);
		buffer.writeByte(this.sentiment.ordinal() & 0xFF);
		buffer.writeByte(this.color.ordinal() & 0xFF);
		buffer.writeVarLong(this.visibleTimeMs);
	}

	public static NotificationStyle decode(FriendlyByteBuf buffer) {
		NotificationIcon icon = NotificationIcon.decode(buffer);
		Sentiment sentiment = Sentiment.VALUES[buffer.readUnsignedByte() % Sentiment.VALUES.length];
		Color color = Color.VALUES[buffer.readUnsignedByte() % Color.VALUES.length];
		long timeMs = buffer.readVarLong();
		return new NotificationStyle(icon, sentiment, color, timeMs);
	}

	public int textureOffset() {
		return this.sentiment.offset + this.color.offset;
	}

	public enum Color {
		DARK("dark", 0),
		LIGHT("light", 32);

		public static final Color[] VALUES = values();
		public static final Codec<Color> CODEC = MoreCodecs.stringVariants(VALUES, c -> c.name);

		private final String name;
		public final int offset;

		Color(String name, int offset) {
			this.name = name;
			this.offset = offset;
		}
	}

	public enum Sentiment {
		NEUTRAL("neutral", 0),
		POSITIVE("positive", 64),
		NEGATIVE("negative", 128);

		public static final Sentiment[] VALUES = values();
		public static final Codec<Sentiment> CODEC = MoreCodecs.stringVariants(VALUES, s -> s.name);

		public final String name;
		public final int offset;

		Sentiment(String name, int offset) {
			this.name = name;
			this.offset = offset;
		}
	}
}
