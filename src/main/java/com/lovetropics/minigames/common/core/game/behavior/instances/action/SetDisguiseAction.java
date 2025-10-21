package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.diguise.DisguiseType;
import com.lovetropics.minigames.common.core.diguise.PlayerDisguise;
import com.lovetropics.minigames.common.core.diguise.ServerPlayerDisguises;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContext;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionParameter;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.component.ResolvableProfile;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public record SetDisguiseAction(DisguiseType disguise, boolean applyDonorName, boolean onlyIfNoDisguise) implements IGameBehavior {
	public static final MapCodec<SetDisguiseAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			DisguiseType.MAP_CODEC.forGetter(SetDisguiseAction::disguise),
			Codec.BOOL.optionalFieldOf("apply_donor_name", false).forGetter(SetDisguiseAction::applyDonorName),
			Codec.BOOL.optionalFieldOf("only_if_no_disguise", false).forGetter(SetDisguiseAction::onlyIfNoDisguise)
	).apply(i, SetDisguiseAction::new));

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final ResourceLocation DUMMY_PLAYER = ResourceLocation.fromNamespaceAndPath("dummyplayers", "dummy_player");

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameActionEvents.APPLY_TO_PLAYER, (context, player) -> {
			PlayerDisguise playerDisguise = PlayerDisguise.getOrNull(player);
			if (playerDisguise == null) {
				return false;
			}
			if (onlyIfNoDisguise && playerDisguise.isDisguised()) {
				return false;
			}
			final CompletableFuture<DisguiseType> future = resolveDisguise(game, context);
			if (ServerPlayerDisguises.set(player, disguise)) {
				// This future might not complete, but we should have already
				future.thenAcceptAsync(resolvedDisguise -> applyResolvedDisguise(player, resolvedDisguise), game.scheduler());
				return true;
			}
			return false;
		});
	}

	private CompletableFuture<DisguiseType> resolveDisguise(final IGamePhase game, final GameActionContext context) {
		final Optional<String> packageSender = context.get(GameActionParameter.PACKAGE_SENDER);
		final DisguiseType.EntityConfig entityDisguise = disguise.entity();
		if (entityDisguise == null) {
			return CompletableFuture.completedFuture(disguise);
		}

		final ResourceLocation id = EntityType.getKey(entityDisguise.type());
		if (applyDonorName && packageSender.isPresent() && DUMMY_PLAYER.equals(id)) {
			return resolveDummyDisguise(game, entityDisguise, packageSender.get()).thenApply(disguise::withEntity);
		}

		return CompletableFuture.completedFuture(disguise);
	}

	private CompletableFuture<DisguiseType.EntityConfig> resolveDummyDisguise(final IGamePhase game, final DisguiseType.EntityConfig entity, final String packageSender) {
		final GameProfileCache profileCache = game.server().getProfileCache();
		if (profileCache == null || !StringUtil.isValidPlayerName(packageSender)) {
			return CompletableFuture.completedFuture(entity);
		}
		final CompletableFuture<DisguiseType.EntityConfig> future = new CompletableFuture<>();
		final CompoundTag nbt = entity.nbt() != null ? entity.nbt().copy() : new CompoundTag();
		profileCache.getAsync(packageSender).thenAcceptAsync(result -> result.ifPresent(profile -> {
			LOGGER.debug("Got profile ID for package sender {}: {}", packageSender, profile.getId());
			nbt.put("profile", ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, new ResolvableProfile(profile)).getOrThrow());
			future.complete(entity.withNbt(nbt));
		}), game.server());
		return future;
	}

	private void applyResolvedDisguise(final ServerPlayer player, final DisguiseType resolvedDisguise) {
		if (disguise == resolvedDisguise) {
			return;
		}
		ServerPlayerDisguises.updateType(player, disguiseType -> {
			if (disguiseType.equals(disguise)) {
				return resolvedDisguise;
			} else {
				LOGGER.debug("Skipping setting resolved disguise on {}, as their disguise has changed", player.getGameProfile().getName());
			}
			return disguiseType;
		});
	}
}
