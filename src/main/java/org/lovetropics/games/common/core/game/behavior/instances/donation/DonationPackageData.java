package org.lovetropics.games.common.core.game.behavior.instances.donation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.StringRepresentable;

import java.util.Optional;

public record DonationPackageData(
		String id,
		PackageType packageType,
		Category category,
		TargetSelectionMode targetSelectionMode,
		boolean applyToTeam,
		Component name,
		Component description,
		Optional<Double> donationAmount
) {
	public static final MapCodec<DonationPackageData> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("id").forGetter(DonationPackageData::id),
			PackageType.CODEC.fieldOf("package_type").forGetter(DonationPackageData::packageType),
			Category.CODEC.fieldOf("category").orElse(Category.OTHER).forGetter(DonationPackageData::category),
			TargetSelectionMode.CODEC.fieldOf("player_select").orElse(TargetSelectionMode.RANDOM).forGetter(DonationPackageData::targetSelectionMode),
			Codec.BOOL.optionalFieldOf("apply_to_team", false).forGetter(DonationPackageData::applyToTeam),
			ComponentSerialization.CODEC.fieldOf("name").forGetter(DonationPackageData::name),
			ComponentSerialization.CODEC.fieldOf("description").forGetter(DonationPackageData::description),
			Codec.DOUBLE.optionalFieldOf("donation_amount").forGetter(DonationPackageData::donationAmount)
	).apply(i, DonationPackageData::new));

	public DonationPackageData apply(PackageCostModifierBehavior.State costModifier) {
		return new DonationPackageData(id, packageType, category, targetSelectionMode, applyToTeam, name, description, donationAmount.map(costModifier::apply));
	}

	public Payload asPayload() {
		return new Payload(this, name.getString(), description.getString());
	}

	public record Payload(
			DonationPackageData data,
			String englishName,
			String englishDesc
	) {
		public static final MapCodec<Payload> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				DonationPackageData.CODEC.forGetter(Payload::data),
				Codec.STRING.fieldOf("english_name").forGetter(Payload::englishName),
				Codec.STRING.fieldOf("english_desc").forGetter(Payload::englishDesc)
		).apply(i, Payload::new));
	}

	public enum PackageType implements StringRepresentable {
		CARE_PACKAGE("care_package", "Care Package"),
		SABOTAGE_PACKAGE("sabotage_package", "Sabotage Package"),
		CHAT_EVENT("chat_event", "Chat Event"),
		;

		public static final Codec<PackageType> CODEC = StringRepresentable.fromEnum(PackageType::values);

		private final String key;
		private final String name;

		PackageType(String key, String name) {
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
	}

	public enum Category implements StringRepresentable {
		LARGE("large"),
		MEDIUM("medium"),
		SMALL("small"),
		OTHER("other"),
		;

		public static final Codec<Category> CODEC = StringRepresentable.fromEnum(Category::values);

		private final String key;

		Category(String key) {
			this.key = key;
		}

		@Override
		public String getSerializedName() {
			return key;
		}
	}
}
