package com.lovetropics.minigames.common.core.game.behavior;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.data.LoveTropicsAttachments;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.integration.game_actions.Donation;
import com.lovetropics.minigames.common.core.integration.state.DonationScale;
import com.lovetropics.minigames.common.core.map.MapRegions;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.lovetropics.peekaboo.api.Disguise;
import org.lovetropics.peekaboo.api.EntityDisguiseHolder;
import org.lovetropics.peekaboo.api.TypedEntityData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class SpawnDonorUtils {

	public static final ResourceLocation DUMMY_PLAYER = ResourceLocation.fromNamespaceAndPath("dummyplayers", "dummy_player");
	public static final DeferredHolder<EntityType<?>, EntityType<?>> DUMMY = DeferredHolder.create(Registries.ENTITY_TYPE, DUMMY_PLAYER);
	private static final List<String> MODEL_MODIFIERS = Util.make(() -> Stream.of(
		"default", "fabulous", "flail", "hovering", "shuffle", "upsidedown", "shrunk", "enlarged", "raised_high_heels", "shruggy_arms", "ender_arms", "stiff_legs", "hop_walk"
	).map(d -> "mm_" + d).toList());

	public static void spawnDonorInRandomRegion(IGamePhase game, final Donation donation, final List<String> regions, List<DonationScale> scales) {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("NoBasePlate", true);

		final DeferredHolder<MobEffect, MobEffect> randomModifierEffect = DeferredHolder.create(Registries.MOB_EFFECT, ResourceLocation.fromNamespaceAndPath("ltextras", Util.getRandom(MODEL_MODIFIERS, game.random())));

		final Villager spawnedMob = EntityType.VILLAGER.create(game.level(), EntitySpawnReason.MOB_SUMMONED);
		if (spawnedMob == null) {
			return;
		}

		if (!donation.minecraftUuid().equals(Util.NIL_UUID)) {
			final ResolvableProfile resolvableProfile = new ResolvableProfile(Optional.empty(), Optional.of(donation.minecraftUuid()), new PropertyMap());
			tag.put("profile", ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, resolvableProfile).getOrThrow());
		}

		if (!DUMMY.isBound()) {
			return;
		}

		DonationScale scale = DonationScale.getScale(donation.amount(), scales);
		final float scaleAmount = (float) scale.scale();
		Disguise disguise = getDisguise(scaleAmount).withEntity(Optional.of(new TypedEntityData(DUMMY.value(), tag)));
		EntityDisguiseHolder.set(spawnedMob, disguise);

		MapRegions mapRegions = game.mapRegions();
		List<BlockBox> regionsToSpawnAt = new ArrayList<>();
		for (String key : regions) {
			regionsToSpawnAt.addAll(mapRegions.get(key));
		}
		BlockBox box = Util.getRandom(regionsToSpawnAt, game.random());
		BlockPos spawnPos = box.sample(game.random());

		spawnedMob.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(
				new AttributeModifier(
						LoveTropics.location("donor_movement_speed"),
						0.1,
						AttributeModifier.Operation.ADD_VALUE
				)
		);
		spawnedMob.addEffect(new MobEffectInstance(randomModifierEffect, MobEffectInstance.INFINITE_DURATION, 0, false, false));
		spawnedMob.setVillagerData(spawnedMob.getVillagerData().withProfession(game.level().registryAccess(), VillagerProfession.NITWIT));
		spawnedMob.refreshBrain(game.level());
		spawnedMob.setCustomName(donation.getDisplayName(scale.color(), game.random()));
		spawnedMob.setData(LoveTropicsAttachments.DONATION, donation);
		spawnedMob.snapTo(spawnPos, 0, 0);
		game.level().addFreshEntity(spawnedMob);
	}

	private static Disguise getDisguise(float scale) {
		return new Disguise(
				Optional.empty(),
				scale,
				true,
				Optional.empty(),
				Optional.empty(),
				false
		);
	}
}
