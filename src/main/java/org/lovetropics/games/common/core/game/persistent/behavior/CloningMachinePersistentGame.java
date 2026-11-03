package org.lovetropics.games.common.core.game.persistent.behavior;

import com.lovetropics.lib.codec.MoreCodecs;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.util.Util;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.persistent.PersistentGame;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehavior;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehaviorType;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehaviors;
import org.lovetropics.games.common.core.game.region.Region;
import org.lovetropics.games.common.core.game.region.RegionQuery;
import org.lovetropics.games.common.util.EntityTemplate;
import org.lovetropics.peekaboo.api.Disguise;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class CloningMachinePersistentGame implements PersistentGameBehavior {

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final Identifier DUMMY_PLAYER = Identifier.fromNamespaceAndPath("dummyplayers", "dummy_player");
	private static final Identifier COLLECTABLE = Identifier.fromNamespaceAndPath("ltextras", "collectible");
	private static final Identifier DISGUISE_COMPONENT_ID = Identifier.fromNamespaceAndPath("peekaboo", "disguise");

	public static final DeferredHolder<EntityType<?>, EntityType<?>> DUMMY = DeferredHolder.create(Registries.ENTITY_TYPE, DUMMY_PLAYER);
	public static final DeferredHolder<EntityType<?>, EntityType<?>> COLLECTABLE_ENTITY = DeferredHolder.create(Registries.ENTITY_TYPE, COLLECTABLE);

	public static final DeferredHolder<DataComponentType<?>, DataComponentType<Disguise>> DISGUISE_COMPONENT = DeferredHolder.create(Registries.DATA_COMPONENT_TYPE, DISGUISE_COMPONENT_ID);

	private final RegionQuery collectableSpawn;
	private final RegionQuery cloningArea;
	private final RegionQuery outputArea;
	private final RegionQuery blockArea;
	private final BlockStateProvider blockStateProvider;
	private final int cloningCooldown;
	private final ItemStackTemplate template;
	private final WeightedList<CompoundTag> additionalEntityData;

	private int cooldown = 0;

	public static final MapCodec<CloningMachinePersistentGame> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegionQuery.CODEC.fieldOf("collectable_spawn").forGetter(c -> c.collectableSpawn),
			RegionQuery.CODEC.fieldOf("cloning_area").forGetter(c -> c.cloningArea),
			RegionQuery.CODEC.fieldOf("output_area").forGetter(c -> c.outputArea),
			RegionQuery.CODEC.optionalFieldOf("block_area", RegionQuery.of()).forGetter(c -> c.blockArea),
			MoreCodecs.BLOCK_STATE_PROVIDER.fieldOf("blocks").forGetter(c -> c.blockStateProvider),
			Codec.INT.optionalFieldOf("cloning_cooldown", 0).forGetter(c -> c.cloningCooldown),
			ItemStackTemplate.CODEC.fieldOf("template").forGetter(c -> c.template),
			WeightedList.codec(CompoundTag.CODEC).optionalFieldOf("additional_entity_data", WeightedList.of()).forGetter(c -> c.additionalEntityData)
	).apply(i, CloningMachinePersistentGame::new));

	public CloningMachinePersistentGame(
			RegionQuery collectableSpawn,
			RegionQuery cloningArea,
			RegionQuery outputArea,
			RegionQuery blockArea,
			BlockStateProvider blockStateProvider,
			int cloningCooldown,
			ItemStackTemplate template,
			WeightedList<CompoundTag> additionalEntityData
	) {
		this.collectableSpawn = collectableSpawn;
		this.cloningArea = cloningArea;
		this.outputArea = outputArea;
		this.blockArea = blockArea;
		this.blockStateProvider = blockStateProvider;
		this.cloningCooldown = cloningCooldown;
		this.template = template;
		this.additionalEntityData = additionalEntityData;
	}

	@Override
	public void register(PersistentGame game, EventRegistrar events) {
		events.listen(GamePhaseEvents.START, (_) -> {
			clearBlockArea(game);
		});

		events.listen(GamePhaseEvents.TICK, () -> {
			if (cooldown > 0) {
				cooldown--;
				if (cooldown == 0) {
					clearBlockArea(game);
				}
			}
			for (ServerPlayer player : game.level().players()) {
				if (cloningArea.contains(game, player)) {
					summonCollectible(game, player);
					cooldown = cloningCooldown;
					return;
				}
			}
		});
	}

	private void fillBlockArea(PersistentGame game) {
		blockArea.forEachBlock(game, (level, pos) ->
				level.setBlock(pos, blockStateProvider.getState(level, level.getRandom(), pos), Block.UPDATE_ALL));
	}

	public void clearBlockArea(PersistentGame game) {
		blockArea.forEachBlock(game, (level, pos) ->
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));
	}

	private void summonCollectible(PersistentGame game, ServerPlayer player) {
		resolveDummyDisguise(player.level().getServer(), player.nameAndId().id()).whenComplete((compoundTag, throwable) -> {
			if (throwable != null) {
				LOGGER.error("Failed to resolve dummy disguise for player {}", player.getName().getString(), throwable);
				return;
			}

			additionalEntityData.getRandom(game.level().getRandom()).ifPresent(compoundTag::merge);
			Disguise disguise = Disguise.NONE.withEntity(Optional.of(TypedEntityData.of(DUMMY.value(), compoundTag)));
			ItemStack stack = template.create();
			stack.set(DISGUISE_COMPONENT.value(), disguise);
			CompoundTag item = new CompoundTag();
			item.put("item", ItemStack.CODEC.encodeStart(player.registryAccess().createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow());
			CompoundTag collectableTag = new CompoundTag();
			collectableTag.put("collectible", item);
			EntityTemplate template = new EntityTemplate(COLLECTABLE_ENTITY.get(), collectableTag);
			Region one = collectableSpawn.one(game);
			collectableSpawn.getEntities(game, entity -> entity.getType() == COLLECTABLE_ENTITY.value()).forEach(Entity::discard);
			Vec3 center = one.center();
			template.spawn(player.level(), center.x, center.y, center.z);
			Vec3 outputCenter = outputArea.one(game).center();
			player.teleportTo(outputCenter.x, outputCenter.y, outputCenter.z);
			fillBlockArea(game);

		});
	}

	@Override
	public Supplier<? extends PersistentGameBehaviorType<?>> type() {
		return PersistentGameBehaviors.CLONING_MACHINE;
	}

	private static CompletableFuture<CompoundTag> resolveDummyDisguise(MinecraftServer server, UUID uuid) {
		ProfileResolver resolver = server.services().profileResolver();
		return CompletableFuture.supplyAsync(() -> resolver.fetchById(uuid), Util.nonCriticalIoPool())
				.thenApplyAsync(maybeProfile -> {
					CompoundTag tag = new CompoundTag();
					maybeProfile.ifPresent(profile -> {
						tag.put("profile", ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, ResolvableProfile.createResolved(profile)).getOrThrow());
					});
					return tag;
				}, server);
	}
}
