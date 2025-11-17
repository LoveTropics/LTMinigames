package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import com.lovetropics.minigames.common.core.integration.BackendIntegrations;
import com.lovetropics.minigames.common.core.integration.GameInstanceIntegrations;
import com.lovetropics.minigames.common.core.integration.state.MinecrafterDonor;
import com.lovetropics.minigames.common.core.map.MapRegions;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
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

public record SpawnDonorsInRegionBehavior(
	List<String> regions
) implements IGameBehavior {

	public static final MapCodec<SpawnDonorsInRegionBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.listOf().fieldOf("regions").forGetter(c -> c.regions)
	).apply(i, SpawnDonorsInRegionBehavior::new));

	public static final ResourceLocation DUMMY_PLAYER = ResourceLocation.fromNamespaceAndPath("dummyplayers", "dummy_player");
	public static final DeferredHolder<EntityType<?>, EntityType<?>> DUMMY = DeferredHolder.create(Registries.ENTITY_TYPE, DUMMY_PLAYER);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		if (!BackendIntegrations.get().isConnected()) {
			throw new GameException(GameTexts.Status.integrationsNotConnected());
		}
		GameInstanceIntegrations integrations = game.instanceState().getOrNull(GameInstanceIntegrations.KEY);
		if (integrations == null) {
			return;
		}

		integrations.get("donations/donors/all", MinecrafterDonor.LIST_CODEC).thenAcceptAsync(result -> {
				if (result.isPresent()) {
					final List<MinecrafterDonor> donors = result.get();
					for (MinecrafterDonor donor : donors) {
						final Villager villager = EntityType.VILLAGER.create(game.level(), EntitySpawnReason.MOB_SUMMONED);
						if (villager == null) {
							return;
						}

						final ResolvableProfile resolvableProfile = new ResolvableProfile(new GameProfile(donor.minecraftUuid(), donor.minecraftName()));
						CompoundTag tag = new CompoundTag();

						if (!donor.minecraftName().isEmpty() && !donor.minecraftUuid().equals(Util.NIL_UUID)) {
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
			}, game.scheduler()
		);
	}
}
