package com.lovetropics.minigames.common.core.diguise;

import com.lovetropics.minigames.LoveTropics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public final class PlayerDisguise {
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, LoveTropics.ID);

	public static final Supplier<AttachmentType<PlayerDisguise>> ATTACHMENT = ATTACHMENT_TYPES.register(
			"player_disguise", () -> AttachmentType.builder(holder -> new PlayerDisguise((LivingEntity) holder))
					.serialize(new IAttachmentSerializer<>() {
						@Override
						public PlayerDisguise read(IAttachmentHolder holder, ValueInput input) {
							PlayerDisguise disguise = new PlayerDisguise((LivingEntity) holder);
							input.read(DisguiseType.MAP_CODEC).ifPresent(disguise::set);
							return disguise;
						}

						@Override
						public boolean write(PlayerDisguise attachment, ValueOutput output) {
							output.store(DisguiseType.MAP_CODEC, attachment.disguise);
							return true;
						}
					}).build()
	);

	private final LivingEntity entity;

	private DisguiseType disguise = DisguiseType.DEFAULT;
	@Nullable
	private Entity disguisedEntity;

	private PlayerDisguise(LivingEntity entity) {
		this.entity = entity;
	}

	@Nullable
	public static PlayerDisguise getOrNull(Entity entity) {
		if (entity instanceof LivingEntity livingEntity) {
			return livingEntity.getData(ATTACHMENT);
		}
		return null;
	}

	public boolean isDisguised() {
		return !type().isDefault();
	}

	public void clear() {
		set(DisguiseType.DEFAULT);
	}

	public void clear(DisguiseType disguise) {
		set(this.disguise.clear(disguise));
	}

	public void set(DisguiseType disguise) {
		if (this.disguise.equals(disguise)) {
			return;
		}
		this.disguise = disguise;
		disguisedEntity = disguise.createEntityFor(entity);
		entity.refreshDimensions();
		PlayerDisguiseBehavior.onDisguiseChange(entity);
	}

	public DisguiseType type() {
		return disguise;
	}

	@Nullable
	public Entity entity() {
		return disguisedEntity;
	}

	public void copyFrom(PlayerDisguise from) {
		set(from.type());
	}

	public float getEffectiveScale() {
		if (disguisedEntity != null) {
			float entityScale = disguisedEntity.getBbHeight() / EntityType.PLAYER.getHeight();
			return disguise.scale() * entityScale;
		} else {
			return disguise.scale();
		}
	}
}
