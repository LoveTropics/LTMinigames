package com.lovetropics.minigames.common.core.diguise;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Optionull;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public record DisguiseType(
		@Nullable EntityConfig entity,
		float scale,
		boolean changesSize,
		@Nullable Component customName,
		@Nullable ResolvableProfile skinProfile
) {
	private static final Logger LOGGER = LogUtils.getLogger();

	public static final DisguiseType DEFAULT = new DisguiseType(
			(EntityConfig) null,
			1.0f,
			true,
			null,
			null
	);

	public static final MapCodec<DisguiseType> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			EntityConfig.CODEC.optionalFieldOf("entity").forGetter(c -> Optional.ofNullable(c.entity)),
			Codec.FLOAT.optionalFieldOf("scale", 1.0f).forGetter(c -> c.scale),
			Codec.BOOL.optionalFieldOf("changes_size", true).forGetter(c -> c.changesSize),
			ComponentSerialization.CODEC.optionalFieldOf("custom_name").forGetter(c -> Optional.ofNullable(c.customName)),
			ResolvableProfile.CODEC.optionalFieldOf("skin_profile").forGetter(c -> Optional.ofNullable(c.skinProfile))
	).apply(i, DisguiseType::new));

	public static final Codec<DisguiseType> CODEC = MAP_CODEC.codec();

	public static final StreamCodec<RegistryFriendlyByteBuf, DisguiseType> STREAM_CODEC = StreamCodec.composite(
			EntityConfig.STREAM_CODEC.apply(ByteBufCodecs::optional), c -> Optional.ofNullable(c.entity),
			ByteBufCodecs.FLOAT, DisguiseType::scale,
			ByteBufCodecs.BOOL, DisguiseType::changesSize,
			ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs::optional), c -> Optional.ofNullable(c.customName),
			ResolvableProfile.STREAM_CODEC.apply(ByteBufCodecs::optional), c -> Optional.ofNullable(c.skinProfile),
			DisguiseType::new
	);

	private DisguiseType(Optional<EntityConfig> entity, float scale, boolean changesSize, Optional<Component> customName, Optional<ResolvableProfile> skinProfile) {
		this(entity.orElse(null), scale, changesSize, customName.orElse(null), skinProfile.orElse(null));
	}

	@Nullable
	public Entity createEntityFor(LivingEntity entity) {
		return createEntity(entity.level());
	}

	@Nullable
	public Entity createEntity(Level level) {
		return entity != null ? entity.createEntity(level) : null;
	}

	public boolean isDefault() {
		return equals(DEFAULT);
	}

	@Nullable
	public EntityType<?> entityType() {
		return entity != null ? entity.type : null;
	}

	public DisguiseType clear(DisguiseType other) {
		DisguiseType clearTo = DEFAULT;
		return new DisguiseType(
				Objects.equals(entity, other.entity) ? clearTo.entity : entity,
				scale == other.scale ? clearTo.scale : scale,
				changesSize == other.changesSize ? clearTo.changesSize : changesSize,
				Objects.equals(customName, other.customName) ? clearTo.customName : customName,
				Objects.equals(skinProfile, other.skinProfile) ? clearTo.skinProfile : skinProfile
		);
	}

	public DisguiseType withEntity(@Nullable EntityConfig entity) {
		if (Objects.equals(entity, this.entity)) {
			return this;
		}
		return new DisguiseType(entity, scale, changesSize, customName, skinProfile);
	}

	public DisguiseType withScale(float scale) {
		if (scale == this.scale) {
			return this;
		}
		return new DisguiseType(entity, scale, changesSize, customName, skinProfile);
	}

	public DisguiseType withCustomName(@Nullable Component customName) {
		if (Objects.equals(customName, this.customName)) {
			return this;
		}
		return new DisguiseType(entity, scale, changesSize, customName, skinProfile);
	}

	public DisguiseType withSkinProfile(@Nullable ResolvableProfile skinProfile) {
		if (Objects.equals(skinProfile, this.skinProfile)) {
			return this;
		}
		return new DisguiseType(entity, scale, changesSize, customName, skinProfile);
	}

	public record EntityConfig(EntityType<?> type, @Nullable CompoundTag nbt, boolean applyAttributes) {
		public static final Codec<EntityConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
				BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("type").forGetter(c -> c.type),
				CompoundTag.CODEC.optionalFieldOf("tag").forGetter(c -> Optional.ofNullable(c.nbt)),
				Codec.BOOL.optionalFieldOf("apply_attributes", false).forGetter(c -> c.applyAttributes)
		).apply(i, EntityConfig::new));

		public static final StreamCodec<RegistryFriendlyByteBuf, EntityConfig> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.registry(Registries.ENTITY_TYPE), EntityConfig::type,
				ByteBufCodecs.OPTIONAL_COMPOUND_TAG, c -> Optional.ofNullable(c.nbt),
				ByteBufCodecs.BOOL, EntityConfig::applyAttributes,
				EntityConfig::new
		);

		private EntityConfig(EntityType<?> type, Optional<CompoundTag> nbt, boolean applyAttributes) {
			this(type, nbt.orElse(null), applyAttributes);
		}

		public EntityConfig withNbt(@Nullable CompoundTag nbt) {
			return new EntityConfig(type, Optionull.map(nbt, CompoundTag::copy), applyAttributes);
		}

		@Nullable
		public Entity createEntity(Level level) {
			try {
				Entity entity = type.create(level, EntitySpawnReason.LOAD);
				if (entity == null) {
					return null;
				}

				if (nbt != null) {
					try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(entity.problemPath(), LOGGER)) {
						entity.load(TagValueInput.create(reporter, entity.registryAccess(), nbt));
					}
				}

				fixInvalidEntity(entity);
				return entity;
			} catch (final Exception e) {
				LOGGER.error("Failed to create entity for disguise: {}", this, e);
				return null;
			}
		}

		private void fixInvalidEntity(Entity entity) {
			entity.stopRiding();
		}
	}
}
