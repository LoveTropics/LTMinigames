package org.lovetropics.games.common.content.qottott.behavior;

import com.lovetropics.lib.BlockBox;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.ActionSubjects;
import org.lovetropics.games.common.core.game.behavior.action.GameActionList;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.player.PlayerRole;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.TriState;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record KitSelectionBehavior(List<Kit> kits) implements IGameBehavior {
	private static final Logger LOGGER = LogUtils.getLogger();

	public static final MapCodec<KitSelectionBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ExtraCodecs.nonEmptyList(Kit.CODEC.listOf()).fieldOf("kits").forGetter(KitSelectionBehavior::kits)
	).apply(i, KitSelectionBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		for (Kit kit : kits) {
			kit.apply.register(game, events);
		}

		Map<UUID, Kit> kitEntities = new Object2ObjectOpenHashMap<>();
		Kit defaultKit = kits.getFirst();

		events.listen(GamePhaseEvents.START, initiator -> {
			for (Kit kit : kits) {
				Collection<BlockBox> regions = game.mapRegions().get(kit.region());
				if (regions.isEmpty()) {
					LOGGER.error("Missing region for kit: {}", kit);
					continue;
				}
				for (BlockBox region : regions) {
					Entity entity = kit.entity().type().create(game.level(), EntitySpawnReason.COMMAND);
					if (entity == null) {
						LOGGER.error("Unable to create entity for kit: {}", kit);
						continue;
					}
					Vec3 center = region.center();
					entity.snapTo(center.x, region.min().getY(), center.z, kit.angle, 0.0f);
					game.level().addFreshEntity(entity);
					kitEntities.put(entity.getUUID(), kit);
				}
			}
		});

		Map<UUID, Kit> selectedKits = new Object2ObjectOpenHashMap<>();
		events.listen(GamePlayerEvents.INTERACT_ENTITY, (player, target, hand) -> applyKit(game, player, target, kitEntities, selectedKits) ? InteractionResult.CONSUME : InteractionResult.PASS);
		events.listen(GamePlayerEvents.ATTACK, (player, target) -> applyKit(game, player, target, kitEntities, selectedKits) ? TriState.TRUE : TriState.DEFAULT);

		events.listen(GamePlayerEvents.SPAWN, (playerId, spawn, role) -> {
			if (role == PlayerRole.PARTICIPANT) {
				spawn.run(player -> {
					Kit kit = selectedKits.getOrDefault(player.getUUID(), defaultKit);
					kit.apply.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player));
				});
			}
		});
	}

	private static boolean applyKit(IGamePhase game, ServerPlayer player, Entity target, Map<UUID, Kit> kitEntities, Map<UUID, Kit> selectedKits) {
		Kit kit = kitEntities.get(target.getUUID());
		if (kit != null) {
			kit.apply.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player));
			selectedKits.put(player.getUUID(), kit);
			return true;
		}
		return false;
	}

	private record Kit(String region, float angle, TypedEntityData<EntityType<?>> entity, GameActionList apply) {
		public static final Codec<Kit> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("region").forGetter(Kit::region),
				Codec.FLOAT.optionalFieldOf("angle", 0.0f).forGetter(Kit::angle),
				TypedEntityData.codec(EntityType.CODEC).fieldOf("entity").forGetter(Kit::entity),
				GameActionList.CODEC.fieldOf("apply").forGetter(Kit::apply)
		).apply(i, Kit::new));
	}
}
