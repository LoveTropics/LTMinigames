package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.SpawnDonorUtils;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionContextKeys;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.util.Util;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.TypedEntityData;
import org.lovetropics.peekaboo.api.Disguise;
import org.lovetropics.peekaboo.api.EntityDisguiseHolder;
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

	private static final Identifier DUMMY_PLAYER = Identifier.fromNamespaceAndPath("dummyplayers", "dummy_player");


	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.applyToPlayers(game, (context, player) -> {
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

	private CompletableFuture<Disguise> resolveDisguise(final IGamePhase game, final ContextMap context) {
		final String packageSender = context.getOptional(GameActionContextKeys.PACKAGE_SENDER);
		final Optional<TypedEntityData<EntityType<?>>> entityDisguise = disguise.entity();
		if (entityDisguise.isEmpty()) {
			return CompletableFuture.completedFuture(disguise);
		}

		final Identifier id = EntityType.getKey(entityDisguise.get().type());
		if (applyDonorName && packageSender != null && DUMMY_PLAYER.equals(id)) {
			return resolveDummyDisguise(game, entityDisguise.get(), packageSender).thenApply(entity -> disguise.withEntity(Optional.of(entity)));
		}

		return CompletableFuture.completedFuture(disguise);
	}

	private CompletableFuture<TypedEntityData<EntityType<?>>> resolveDummyDisguise(final IGamePhase game, final TypedEntityData<EntityType<?>> entity, final String packageSender) {
		CompletableFuture<TypedEntityData<EntityType<?>>> future = new CompletableFuture<>();
		Util.getProfile(game.server(), packageSender).thenAcceptAsync(result -> result.ifPresent(profile -> {
			LOGGER.debug("Got profile ID for package sender {}: {}", packageSender, profile.id());
			CompoundTag tag = entity.getUnsafe().copy();
			tag.put("profile", ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, ResolvableProfile.createResolved(profile)).getOrThrow());
			future.complete(TypedEntityData.of(entity.type(), tag));
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
				LOGGER.debug("Skipping setting resolved disguise on {}, as their disguise has changed", player.getGameProfile().name());
			}
			return disguise;
		});
	}
}
