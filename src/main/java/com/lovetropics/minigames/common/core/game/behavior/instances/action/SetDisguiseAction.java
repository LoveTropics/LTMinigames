package com.lovetropics.minigames.common.core.game.behavior.instances.action;

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
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.component.ResolvableProfile;
import org.lovetropics.peekaboo.api.Disguise;
import org.lovetropics.peekaboo.api.EntityDisguiseHolder;
import org.lovetropics.peekaboo.api.TypedEntityData;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public record SetDisguiseAction(Disguise disguise, boolean applyDonorName, boolean onlyIfNoDisguise) implements IGameBehavior {
	public static final MapCodec<SetDisguiseAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Disguise.MAP_CODEC.forGetter(SetDisguiseAction::disguise),
			Codec.BOOL.optionalFieldOf("apply_donor_name", false).forGetter(SetDisguiseAction::applyDonorName),
			Codec.BOOL.optionalFieldOf("only_if_no_disguise", false).forGetter(SetDisguiseAction::onlyIfNoDisguise)
	).apply(i, SetDisguiseAction::new));

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final ResourceLocation DUMMY_PLAYER = ResourceLocation.fromNamespaceAndPath("dummyplayers", "dummy_player");

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameActionEvents.APPLY_TO_PLAYER, (context, player) -> {
			EntityDisguiseHolder disguiseHolder = EntityDisguiseHolder.getOrNull(player);
			if (disguiseHolder == null) {
				return false;
			}
			if (onlyIfNoDisguise && !disguiseHolder.disguise().isEmpty()) {
				return false;
			}
			final CompletableFuture<Disguise> future = resolveDisguise(game, context);
			disguiseHolder.set(disguise);
			// This future might not complete, but we should have already
			future.thenAcceptAsync(resolvedDisguise -> applyResolvedDisguise(player, resolvedDisguise), game.scheduler());
			return true;
		});
	}

	private CompletableFuture<Disguise> resolveDisguise(final IGamePhase game, final GameActionContext context) {
		final Optional<String> packageSender = context.get(GameActionParameter.PACKAGE_SENDER);
		final Optional<TypedEntityData> entityDisguise = disguise.entity();
		if (entityDisguise.isEmpty()) {
			return CompletableFuture.completedFuture(disguise);
		}

		final ResourceLocation id = EntityType.getKey(entityDisguise.get().type());
		if (applyDonorName && packageSender.isPresent() && DUMMY_PLAYER.equals(id)) {
			return resolveDummyDisguise(game, entityDisguise.get(), packageSender.get()).thenApply(entity -> disguise.withEntity(Optional.of(entity)));
		}

		return CompletableFuture.completedFuture(disguise);
	}

	private CompletableFuture<TypedEntityData> resolveDummyDisguise(final IGamePhase game, final TypedEntityData entity, final String packageSender) {
		final GameProfileCache profileCache = game.server().getProfileCache();
		if (profileCache == null || !StringUtil.isValidPlayerName(packageSender)) {
			return CompletableFuture.completedFuture(entity);
		}
		final CompletableFuture<TypedEntityData> future = new CompletableFuture<>();
		profileCache.getAsync(packageSender).thenAcceptAsync(result -> result.ifPresent(profile -> {
			LOGGER.debug("Got profile ID for package sender {}: {}", packageSender, profile.getId());
			future.complete(new TypedEntityData(entity.data().update(tag ->
					tag.put("profile", ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, new ResolvableProfile(profile)).getOrThrow())
			)));
		}), game.server());
		return future;
	}

	private void applyResolvedDisguise(final ServerPlayer player, final Disguise resolvedDisguise) {
		if (disguise == resolvedDisguise) {
			return;
		}
		EntityDisguiseHolder.update(player, disguise -> {
			if (disguise.equals(this.disguise)) {
				return resolvedDisguise;
			} else {
				LOGGER.debug("Skipping setting resolved disguise on {}, as their disguise has changed", player.getGameProfile().getName());
			}
			return disguise;
		});
	}
}
