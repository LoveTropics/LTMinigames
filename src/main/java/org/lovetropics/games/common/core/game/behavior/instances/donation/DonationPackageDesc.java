package org.lovetropics.games.common.core.game.behavior.instances.donation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.StringRepresentable;
import org.lovetropics.games.client.toast.NotificationStyle;

import java.util.Optional;

/// An external description of a Donation Package, to be ingested by the Techstack.
public record DonationPackageDesc(
		String id,
		Type type,
		SelectionMode selectionMode,
		Component name,
		String englishName,
		Component description,
		String englishDesc,
		Optional<Double> donationAmount
) {
	public static final Codec<DonationPackageDesc> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("id").forGetter(DonationPackageDesc::id),
			Type.CODEC.fieldOf("package_type").forGetter(DonationPackageDesc::type),
			SelectionMode.CODEC.fieldOf("select").forGetter(DonationPackageDesc::selectionMode),
			ComponentSerialization.CODEC.fieldOf("name").forGetter(DonationPackageDesc::name),
			Codec.STRING.fieldOf("english_name").forGetter(DonationPackageDesc::englishName),
			ComponentSerialization.CODEC.fieldOf("description").forGetter(DonationPackageDesc::description),
			Codec.STRING.fieldOf("english_desc").forGetter(DonationPackageDesc::englishDesc),
			Codec.DOUBLE.optionalFieldOf("donation_amount").forGetter(DonationPackageDesc::donationAmount)
	).apply(i, DonationPackageDesc::new));

	public enum Type implements StringRepresentable {
		CARE_PACKAGE("care_package", "Care Package"),
		SABOTAGE_PACKAGE("sabotage_package", "Sabotage Package"),
		CHAT_EVENT("chat_event", "Chat Event"),
		AUTO("auto", "Auto"),
		;

		public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

		private final String key;
		private final String name;

		Type(String key, String name) {
			this.key = key;
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return key;
		}

		public String getName() {
			return name;
		}

		public NotificationStyle.Sentiment defaultSentiment() {
			return switch (this) {
				case CARE_PACKAGE -> NotificationStyle.Sentiment.POSITIVE;
				case SABOTAGE_PACKAGE -> NotificationStyle.Sentiment.NEGATIVE;
				case CHAT_EVENT, AUTO -> NotificationStyle.Sentiment.NEUTRAL;
			};
		}
	}

	public enum SelectionMode implements StringRepresentable {
		NONE("none"),
		PLAYER("player"),
		TEAM("team"),
		;

		public static final Codec<SelectionMode> CODEC = StringRepresentable.fromEnum(SelectionMode::values);

		private final String name;

		SelectionMode(String name) {
			this.name = name;
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}
}
