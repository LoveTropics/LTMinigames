package com.lovetropics.minigames.common.content.qottott.behavior;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.diguise.DisguiseType;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContext;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
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
	public void register(final IGamePhase game, final EventRegistrar events) {
		for (final Kit kit : kits) {
			kit.apply.register(game, events);
		}

		final Map<UUID, Kit> kitEntities = new Object2ObjectOpenHashMap<>();
		final Kit defaultKit = kits.get(0);

		events.listen(GamePhaseEvents.START, () -> {
			for (final Kit kit : kits) {
				final Collection<BlockBox> regions = game.getMapRegions().get(kit.region());
				if (regions.isEmpty()) {
					LOGGER.error("Missing region for kit: {}", kit);
					continue;
				}
				for (final BlockBox region : regions) {
					final Entity entity = kit.entity().createEntity(game.getLevel());
					if (entity == null) {
						LOGGER.error("Unable to create entity for kit: {}", kit);
						continue;
					}
					final Vec3 center = region.center();
					entity.moveTo(center.x, region.min().getY(), center.z, kit.angle, 0.0f);
					game.getLevel().addFreshEntity(entity);
					kitEntities.put(entity.getUUID(), kit);
				}
			}
		});

		final Map<UUID, Kit> selectedKits = new Object2ObjectOpenHashMap<>();
		events.listen(GamePlayerEvents.INTERACT_ENTITY, (player, target, hand) -> applyKit(game, player, target, kitEntities, selectedKits));
		events.listen(GamePlayerEvents.ATTACK, (player, target) -> applyKit(game, player, target, kitEntities, selectedKits));

		events.listen(GamePlayerEvents.SPAWN, (playerId, spawn, role) -> {
			if (role == PlayerRole.PARTICIPANT) {
				spawn.run(player -> {
					final Kit kit = selectedKits.getOrDefault(player.getUUID(), defaultKit);
					kit.apply.apply(game, GameActionContext.EMPTY, player);
				});
			}
		});
	}

	private static InteractionResult applyKit(final IGamePhase game, final ServerPlayer player, final Entity target, final Map<UUID, Kit> kitEntities, final Map<UUID, Kit> selectedKits) {
		final Kit kit = kitEntities.get(target.getUUID());
		if (kit != null) {
			kit.apply.apply(game, GameActionContext.EMPTY, player);
			selectedKits.put(player.getUUID(), kit);
			return InteractionResult.CONSUME;
		}
		return InteractionResult.PASS;
	}

	private record Kit(String region, float angle, DisguiseType.EntityConfig entity, GameActionList<ServerPlayer> apply) {
		public static final Codec<Kit> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("region").forGetter(Kit::region),
				Codec.FLOAT.optionalFieldOf("angle", 0.0f).forGetter(Kit::angle),
				DisguiseType.EntityConfig.CODEC.fieldOf("entity").forGetter(Kit::entity),
				GameActionList.PLAYER_CODEC.fieldOf("apply").forGetter(Kit::apply)
		).apply(i, Kit::new));
	}
}
