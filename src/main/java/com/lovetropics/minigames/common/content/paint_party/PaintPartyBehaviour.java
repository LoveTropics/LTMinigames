package com.lovetropics.minigames.common.content.paint_party;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.paint_party.entity.PaintBallEntity;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GameWorldEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.lovetropics.minigames.common.core.network.SetForcedPoseMessage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;

import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public record PaintPartyBehaviour(Map<GameTeamKey, TeamConfig> teamConfigs, BlockState neutralBlock, int startAmmo, int ammoRechargeTicks) implements IGameBehavior {
	public static final MapCodec<PaintPartyBehaviour> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.unboundedMap(GameTeamKey.CODEC, TeamConfig.CODEC).optionalFieldOf("teams", Map.of()).forGetter(b -> b.teamConfigs),
			MoreCodecs.BLOCK_STATE.optionalFieldOf("neutral_block", Blocks.CONCRETE.white().defaultBlockState()).forGetter(PaintPartyBehaviour::neutralBlock),
			Codec.INT.optionalFieldOf("starting_ammo", 64).forGetter(PaintPartyBehaviour::startAmmo),
			Codec.INT.optionalFieldOf("ammo_recharge_ticks", 2).forGetter(PaintPartyBehaviour::ammoRechargeTicks)
	).apply(i, PaintPartyBehaviour::new));
	private static final Holder<EntityType<?>> EXPLODING_COCONUT = DeferredHolder.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("tropicraft", "exploding_coconut"));

	private static final AttributeModifier SWIMMING_SPEED_MODIFIER = new AttributeModifier(
			LoveTropics.id("paint_party/swimming_in_paint"),
			0.5,
			AttributeModifier.Operation.ADD_VALUE
	);

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		Map<GameTeamKey, BlockBox> spawnRegions = teamConfigs.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, teamEntry -> game.mapRegions().getOrThrow(teamEntry.getValue().spawnRegion())));

		events.listen(GamePlayerEvents.SPAWN, (playerId, spawn, role) -> {
			if (role == PlayerRole.PARTICIPANT) {
				GameTeamKey teamKey = teams.getTeamForPlayer(playerId);
				if (teamKey == null) {
					return;
				}
				TeamConfig teamConfig = teamConfigs.get(teamKey);
				spawn.run(player ->
						player.getInventory().add(teamConfig.ammoItem.create().copyWithCount(startAmmo))
				);
			}
		});
		events.listen(GameWorldEvents.EXPLOSION_DETONATE, (explosion, affectedBlocks, affectedEntities) -> {
			if ((explosion.getDirectSourceEntity().is(EXPLODING_COCONUT) || explosion.getDirectSourceEntity() instanceof PaintBallEntity) && explosion.getIndirectSourceEntity() instanceof ServerPlayer throwingPlayer) {
				GameTeamKey teamKey = teams.getTeamForPlayer(throwingPlayer);
				if (teamKey == null) {
					return;
				}
				TeamConfig teamConfig = getTeamConfig(teamKey);
				for (BlockPos pos : affectedBlocks) {
					BlockState blockState = game.level().getBlockState(pos);
					if (!blockState.isAir() && blockState.is(Tags.Blocks.DYED) && !blockState.is(teamConfig.blockTag())) {
						paintBlock(game, pos, teamKey, teamConfig, blockState);
					}
				}
				affectedBlocks.clear();
				affectedEntities.clear();
			}
		});
		events.listen(GamePlayerEvents.TICK, (player) -> {
			// Persist whatever state the player already has if they are not on the ground
			if (!player.onGround()) {
				return;
			}
			GameTeamKey teamKey = teams.getTeamForPlayer(player);
			if (teamKey == null) {
				return;
			}
			BlockPos playerPos = player.getOnPos();
			if (isInOpponentSpawnRegion(spawnRegions, teamKey, playerPos)) {
				return;
			}
			TeamConfig teamConfig = getTeamConfig(teamKey);
			BlockState blockState = game.level().getBlockState(playerPos);
			if (!blockState.isAir() && blockState.is(Tags.Blocks.DYED)) {
				if (!blockState.is(teamConfig.blockTag())) {
					tickOnOpponentTeamBlock(game, player, playerPos, teamConfig, blockState, teamKey);
				} else {
					tickOnOwnTeamBlock(game, player, teamConfig, playerPos);
				}
			} else if (player.getForcedPose() == Pose.SWIMMING) {
				player.setForcedPose(null);
				PacketDistributor.sendToPlayer(player, new SetForcedPoseMessage(Optional.empty()));
			}

			if (player.isVisuallySwimming()) {
				player.getAttribute(Attributes.MOVEMENT_SPEED).addOrUpdateTransientModifier(SWIMMING_SPEED_MODIFIER);
			} else {
				player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SWIMMING_SPEED_MODIFIER);
			}
		});

		events.listen(GamePlayerEvents.USE_ITEM, (player, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			GameTeamKey teamKey = teams.getTeamForPlayer(player);
			if (teamKey == null) {
				return InteractionResult.PASS;
			}

			TeamConfig teamConfig = getTeamConfig(teamKey);

			if (stack.is(Items.DIAMOND_HOE) && player.getInventory().countItem(teamConfig.ammoItem.item().value()) >= 20) {
				removeFromInventory(player, teamConfig, 20);

				player.getCooldowns().addCooldown(stack, 60);

				PaintBallEntity paintball = new PaintBallEntity(player.level());
				paintball.setOwner(player);
				paintball.setPos(player.getX(), player.getEyeY() - 0.1F, player.getZ());
				paintball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 0.75F, 1.0F);
				paintball.setVisualItem(teamConfig.ammoItem.create());
				player.level().addFreshEntity(paintball);

				return InteractionResult.PASS;
			}

			return InteractionResult.PASS;
		});

		events.listen(PaintPartyEvents.PAINTBALL_HIT, (level, entity, pos) -> {
			Entity owner = entity.getOwner();
			if (!(owner instanceof ServerPlayer player)) {
				return;
			}

			GameTeamKey teamKey = teams.getTeamForPlayer(player);
			if (teamKey == null) {
				return;
			}
			BlockState state = level.getBlockState(pos);

			float radius = 2.5f;
			if (state.is(neutralBlock.getBlock()) || state.is(getTeamConfig(teamKey).blockType.getBlock())) {
				radius = 4f;
			}

			Vec3 center = entity.position().add(0.0, 1.0, 0.0);
			ServerExplosion explosion = new ServerExplosion((ServerLevel) level, entity, null, null, center, radius, false, Explosion.BlockInteraction.KEEP);
			explosion.explode();
		});
	}

	// TODO: taken from bioblitz, abstract out into helper eventually!

	private int removeFromInventory(ServerPlayer player, TeamConfig config, int amount) {
		int remaining = amount;

		List<Slot> slots = player.containerMenu.slots;
		for (Slot slot : slots) {
			remaining -= removeFromSlot(slot, i -> i.is(config.ammoItem().item()), remaining);
			if (remaining <= 0) {
				break;
			}
		}

		remaining -= ContainerHelper.clearOrCountMatchingItems(player.containerMenu.getCarried(), i -> i.is(config.ammoItem().item()), remaining, false);

		player.containerMenu.broadcastChanges();

		return amount - remaining;
	}

	private int removeFromSlot(Slot slot, Predicate<ItemStack> predicate, int amount) {
		ItemStack stack = slot.getItem();
		if (predicate.test(stack)) {
			int removed = Math.min(amount, stack.getCount());
			stack.shrink(removed);
			return removed;
		}

		return 0;
	}

	private static boolean isInOpponentSpawnRegion(Map<GameTeamKey, BlockBox> spawnRegions, GameTeamKey teamKey, BlockPos playerPos) {
		for (Map.Entry<GameTeamKey, BlockBox> entry : spawnRegions.entrySet()) {
			if (!entry.getKey().equals(teamKey) && entry.getValue().contains(playerPos)) {
				return true;
			}
		}
		return false;
	}

	private void tickOnOwnTeamBlock(IGamePhase game, ServerPlayer player, TeamConfig teamConfig, BlockPos playerPos) {
		// If we're swimming, reload.
		if (player.getForcedPose() == Pose.SWIMMING) {
			if (!player.isShiftKeyDown()) {
				player.setForcedPose(null);
				PacketDistributor.sendToPlayer(player, new SetForcedPoseMessage(Optional.empty()));
			} else {
				if (game.ticks() % ammoRechargeTicks() == 0) {
					if (player.getInventory().countItem(teamConfig.ammoItem.item().value()) < startAmmo) {
						player.getInventory().add(teamConfig.ammoItem.create());
						BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, teamConfig.blockType(), playerPos);
						game.level().sendParticles(particle, playerPos.getX() + 0.5, playerPos.getY() + 1.5, playerPos.getZ() + 0.5, 150, 0, 0, 0, 0.15F);
					}
				}
			}
		} else if (player.isShiftKeyDown()) {
			if (player.getForcedPose() != Pose.SWIMMING) {
				player.setForcedPose(Pose.SWIMMING);
				PacketDistributor.sendToPlayer(player, new SetForcedPoseMessage(Optional.of(Pose.SWIMMING)));
			}
		}
	}

	private void tickOnOpponentTeamBlock(IGamePhase game, ServerPlayer player, BlockPos playerPos, TeamConfig teamConfig, BlockState blockState, GameTeamKey teamKey) {
		if (player.isSwimming() || player.getForcedPose() == Pose.SWIMMING) {
			player.setSwimming(false);
			player.setShiftKeyDown(false);
			player.setForcedPose(null);
			PacketDistributor.sendToPlayer(player, new SetForcedPoseMessage(Optional.empty()));
			return;
		}
		if (!hasNeighboringPaint(game, playerPos, teamConfig)) {
			return;
		}
		// Check if player has any blocks to place
		if (player.getInventory().hasAnyMatching(itemStack -> itemStack.is(teamConfig.itemTag()))) {
			// Player has blocks to place
			paintBlock(game, playerPos, teamKey, teamConfig, blockState);
			removeFromInventory(player, teamConfig, 1);
		}
	}

	private void paintBlock(IGamePhase game, BlockPos pos, GameTeamKey teamKey, TeamConfig team, BlockState previousState) {
		if (!previousState.equals(neutralBlock)) {
			// Enemy team block, remove score from enemy
			GameTeamKey teamFromBlockState = getTeamFromBlockState(previousState);
			if (teamFromBlockState != null) {
				game.statistics().forTeam(teamFromBlockState).incrementInt(StatisticKey.POINTS, -1);
			}
		}
		game.level().setBlock(pos, team.blockType(), Block.UPDATE_CLIENTS);
		game.level().sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, team.blockType(), pos), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 100, 0, 0, 0, 0.15F);
		game.statistics().forTeam(teamKey).incrementInt(StatisticKey.POINTS, 1);
	}

	private static boolean hasNeighboringPaint(IGamePhase game, BlockPos pos, TeamConfig teamConfig) {
		for (BlockPos neighborPos : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
			if (neighborPos.equals(pos)) {
				continue;
			}
			if (game.level().getBlockState(neighborPos).is(teamConfig.blockTag())) {
				return true;
			}
		}
		return false;
	}

	public TeamConfig getTeamConfig(GameTeamKey teamKey) {
		return teamConfigs.get(teamKey);
	}

	public @Nullable GameTeamKey getTeamFromBlockState(BlockState blockState) {
		for (Map.Entry<GameTeamKey, TeamConfig> entry : teamConfigs.entrySet()) {
			if (entry.getValue().blockType.equals(blockState)) {
				return entry.getKey();
			}
		}
		return null;
	}

	public record TeamConfig(
			String spawnRegion,
			BlockState blockType,
			ItemStackTemplate ammoItem,
			TagKey<Block> blockTag,
			TagKey<Item> itemTag
	) {
		public static final Codec<TeamConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("spawn_region").forGetter(TeamConfig::spawnRegion),
				MoreCodecs.BLOCK_STATE.fieldOf("block").forGetter(TeamConfig::blockType),
				ItemStackTemplate.CODEC.fieldOf("ammo_item").forGetter(TeamConfig::ammoItem),
				TagKey.hashedCodec(Registries.BLOCK).fieldOf("block_tag").forGetter(TeamConfig::blockTag),
				TagKey.hashedCodec(Registries.ITEM).fieldOf("item_tag").forGetter(TeamConfig::itemTag)
		).apply(i, TeamConfig::new));
	}
}
