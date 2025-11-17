package com.lovetropics.minigames.common.core.game.behavior;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.integration.state.MinecrafterDonor;
import com.lovetropics.minigames.common.core.map.MapRegions;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.lovetropics.peekaboo.api.Disguise;
import org.lovetropics.peekaboo.api.EntityDisguiseHolder;
import org.lovetropics.peekaboo.api.TypedEntityData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SpawnDonorUtils {

	public static final ResourceLocation DUMMY_PLAYER = ResourceLocation.fromNamespaceAndPath("dummyplayers", "dummy_player");
	public static final DeferredHolder<EntityType<?>, EntityType<?>> DUMMY = DeferredHolder.create(Registries.ENTITY_TYPE, DUMMY_PLAYER);

	public static void spawnDonorInRandomRegion(IGamePhase game, final MinecrafterDonor donor, final List<String> regions) {
		CompoundTag tag = new CompoundTag();
		final Villager villager = EntityType.VILLAGER.create(game.level(), EntitySpawnReason.MOB_SUMMONED);
		if (villager == null) {
			return;
		}

		final String customName = donor.minecraftName().isEmpty() ? donor.name() : donor.minecraftName();
		villager.setCustomName(Component.literal(customName));

		if (!donor.minecraftUuid().equals(Util.NIL_UUID)) {
			final ResolvableProfile resolvableProfile = new ResolvableProfile(Optional.empty(), Optional.of(donor.minecraftUuid()), new PropertyMap());
			tag.put("profile", ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, resolvableProfile).getOrThrow());
		}

		if (!DUMMY.isBound()) {
			return;
		}
		Disguise disguise = Disguise.NONE.withEntity(Optional.of(new TypedEntityData(DUMMY.value(), tag)));
		EntityDisguiseHolder.set(villager, disguise);

		MapRegions mapRegions = game.mapRegions();
		List<BlockBox> regionsToSpawnAt = new ArrayList<>();
		for (String key : regions) {
			regionsToSpawnAt.addAll(mapRegions.get(key));
		}
		BlockBox box = Util.getRandom(regionsToSpawnAt, game.random());
		BlockPos spawnPos = box.sample(game.random());

		villager.snapTo(spawnPos, 0, 0);
		game.level().addFreshEntity(villager);
	}
}
