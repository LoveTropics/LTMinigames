package com.lovetropics.minigames.common.core.game.behavior;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.data.LoveTropicsAttachments;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.integration.game_actions.Donation;
import com.lovetropics.minigames.common.core.integration.state.DonationScale;
import com.lovetropics.minigames.common.core.map.MapRegions;
import com.mojang.logging.LogUtils;
import net.minecraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.TypedEntityData;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.lovetropics.peekaboo.api.Disguise;
import org.lovetropics.peekaboo.api.EntityDisguiseHolder;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SpawnDonorUtils {

	private static final Logger LOGGER = LogUtils.getLogger();

	public static final Identifier DUMMY_PLAYER = Identifier.fromNamespaceAndPath("dummyplayers", "dummy_player");
	public static final DeferredHolder<EntityType<?>, EntityType<?>> DUMMY = DeferredHolder.create(Registries.ENTITY_TYPE, DUMMY_PLAYER);

	public static void spawnDonorInRandomRegion(IGamePhase game, final Donation donation, final List<String> regions, List<DonationScale> scales, List<ItemStack> bootItems) {
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("NoBasePlate", true);

		final Villager spawnedMob = EntityTypes.VILLAGER.create(game.level(), EntitySpawnReason.MOB_SUMMONED);
		if (spawnedMob == null) {
			return;
		}

		if (!donation.minecraftUuid().equals(Util.NIL_UUID)) {
			final ResolvableProfile resolvableProfile = ResolvableProfile.createUnresolved(donation.minecraftUuid());
			tag.put("profile", ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, resolvableProfile).getOrThrow());
		}

		if (!DUMMY.isBound()) {
			return;
		}

		DonationScale scale = DonationScale.getScale(donation.amount(), scales);
		final float scaleAmount = (float) scale.scale();
		Disguise disguise = getDisguise(scaleAmount).withEntity(Optional.of(TypedEntityData.of(DUMMY.value(), tag)));
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
						LoveTropics.id("donor_movement_speed"),
						0.1,
						AttributeModifier.Operation.ADD_VALUE
				)
		);

		Util.getRandomSafe(bootItems, game.random()).ifPresent(itemStack -> spawnedMob.setItemSlot(EquipmentSlot.FEET, itemStack));

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
