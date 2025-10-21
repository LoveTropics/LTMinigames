package com.lovetropics.minigames.common.core.diguise;

import com.google.common.collect.Iterables;
import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.network.PlayerDisguiseMessage;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static com.mojang.brigadier.arguments.FloatArgumentType.floatArg;
import static com.mojang.brigadier.arguments.FloatArgumentType.getFloat;
import static net.minecraft.commands.Commands.*;
import static net.minecraft.commands.arguments.CompoundTagArgument.compoundTag;
import static net.minecraft.commands.arguments.CompoundTagArgument.getCompoundTag;

@EventBusSubscriber(modid = LoveTropics.ID)
public final class ServerPlayerDisguises {
	private static final SimpleCommandExceptionType NOT_LIVING_ENTITY = new SimpleCommandExceptionType(Component.literal("Not a living entity"));
	private static final SimpleCommandExceptionType ONLY_ONE_PLAYER = new SimpleCommandExceptionType(Component.literal("Can only "));

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		// @formatter:off
		event.getDispatcher().register(
			literal("disguise").requires(source -> source.hasPermission(LEVEL_GAMEMASTERS))
				.then(literal("as")
					.then(argument("entity", ResourceArgument.resource(event.getBuildContext(), Registries.ENTITY_TYPE))
						.suggests(SuggestionProviders.cast(SuggestionProviders.SUMMONABLE_ENTITIES))
						.executes(context -> disguiseAsEntity(context, ResourceArgument.getSummonableEntityType(context, "entity"), null))
							.then(argument("nbt", compoundTag())
								.executes(context -> disguiseAsEntity(context, ResourceArgument.getSummonableEntityType(context, "entity"), getCompoundTag(context, "nbt")))
							)
					)
				)
				.then(literal("skin")
						.then(literal("as")
								.then(argument("player", GameProfileArgument.gameProfile())
										.executes(context -> disguiseSkin(context, GameProfileArgument.getGameProfiles(context, "player")))
								)
						)
						.then(literal("clear")
								.executes(ServerPlayerDisguises::clearSkin)
						)
				)
				.then(literal("name")
						.then(literal("as")
								.then(argument("name", ComponentArgument.textComponent(event.getBuildContext()))
										.executes(context -> disguiseName(context, ComponentArgument.getResolvedComponent(context, "name")))
								)
						)
						.then(literal("clear")
								.executes(ServerPlayerDisguises::clearName)
						)
				)
				.then(literal("scale")
					.then(argument("scale", floatArg(0.1f, 20.0f))
						.executes(context -> disguiseScale(context, getFloat(context, "scale")))
					)
				)
				.then(literal("clear")
					.executes(ServerPlayerDisguises::clearDisguise)
				)
		);
		// @formatter:on
	}

	@SubscribeEvent
	public static void onEntityTrack(PlayerEvent.StartTracking event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			return;
		}

		Entity tracked = event.getTarget();
		sendDisguiseTo(player, tracked);
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof final ServerPlayer player) {
			sendDisguiseTo(player, player);
		}
	}

	@SubscribeEvent
	public static void onPlayerChangeDimension(final PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof final ServerPlayer player) {
			sendDisguiseTo(player, player);
		}
	}

	public static void sendDisguiseTo(ServerPlayer player, Entity tracked) {
		PlayerDisguise disguise = PlayerDisguise.getOrNull(tracked);
		if (disguise != null && disguise.isDisguised()) {
			PacketDistributor.sendToPlayer(
					player,
					new PlayerDisguiseMessage(tracked.getId(), Optional.of(disguise.type()))
			);
		}
	}

	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		if (event.getEntity() instanceof ServerPlayer newPlayer) {
			onSetDisguise(newPlayer);
		}
	}

	private static int disguiseAsEntity(CommandContext<CommandSourceStack> context, Holder.Reference<EntityType<?>> entity, @Nullable CompoundTag nbt) throws CommandSyntaxException {
		updateType(getLivingEntity(context),
				type -> type.withEntity(new DisguiseType.EntityConfig(entity.value(), nbt, false))
		);
		return Command.SINGLE_SUCCESS;
	}

	private static int disguiseSkin(CommandContext<CommandSourceStack> context, Collection<GameProfile> profiles) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		if (profiles.size() != 1) {
			throw EntityArgument.ERROR_NOT_SINGLE_PLAYER.create();
		}

		GameProfile sourceProfile = Iterables.getOnlyElement(profiles);
		CompletableFuture<ResolvableProfile> future = SkullBlockEntity.fetchGameProfile(sourceProfile.getId())
				.thenApply(optional -> new ResolvableProfile(optional.orElse(sourceProfile)));

		if (future.isDone()) {
			updateType(player, disguise -> disguise.withSkinProfile(future.join()));
			return Command.SINGLE_SUCCESS;
		}

		// Just put something in there for now, and replace it later
		DisguiseType temporaryDisguise = updateType(player, disguise -> disguise.withSkinProfile(new ResolvableProfile(sourceProfile)));
		future.thenAcceptAsync(
				resolvedProfile -> updateType(player, replacedDisguise -> {
					// The skin changed again before we resolved it, don't replace
					if (!Objects.equals(replacedDisguise.skinProfile(), temporaryDisguise.skinProfile())) {
						return replacedDisguise;
					}
					return replacedDisguise.withSkinProfile(resolvedProfile);
				}),
				context.getSource().getServer()
		);

		return Command.SINGLE_SUCCESS;
	}

	private static int clearSkin(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		updateType(getLivingEntity(context), d -> d.withSkinProfile(null));
		return Command.SINGLE_SUCCESS;
	}

	private static int disguiseName(CommandContext<CommandSourceStack> context, Component name) throws CommandSyntaxException {
		LivingEntity entity = getLivingEntity(context);
		updateType(entity, d -> d.withCustomName(name));
		return Command.SINGLE_SUCCESS;
	}

	private static int clearName(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		updateType(getLivingEntity(context), d -> d.withCustomName(null));
		return Command.SINGLE_SUCCESS;
	}

	private static int disguiseScale(CommandContext<CommandSourceStack> context, float scale) throws CommandSyntaxException {
		updateType(getLivingEntity(context), d -> d.withScale(scale));
		return Command.SINGLE_SUCCESS;
	}

	private static int clearDisguise(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayerDisguises.set(getLivingEntity(context), DisguiseType.DEFAULT);

		return Command.SINGLE_SUCCESS;
	}

	public static boolean set(LivingEntity entity, DisguiseType type) {
		PlayerDisguise disguise = PlayerDisguise.getOrNull(entity);
		if (disguise != null) {
			disguise.set(type);
			onSetDisguise(entity);
			return true;
		}
		return false;
	}

	public static void update(LivingEntity entity, Consumer<PlayerDisguise> consumer) {
		PlayerDisguise disguise = PlayerDisguise.getOrNull(entity);
		if (disguise != null) {
			consumer.accept(disguise);
			onSetDisguise(entity);
		}
	}

	public static DisguiseType updateType(LivingEntity entity, UnaryOperator<DisguiseType> operator) {
		PlayerDisguise disguise = PlayerDisguise.getOrNull(entity);
		if (disguise != null) {
			DisguiseType newDisguise = operator.apply(disguise.type());
			disguise.set(newDisguise);
			onSetDisguise(entity);
			return newDisguise;
		}
		return DisguiseType.DEFAULT;
	}

	public static void clear(LivingEntity entity) {
		update(entity, PlayerDisguise::clear);
	}

	public static void clear(LivingEntity entity, DisguiseType type) {
		update(entity, disguise -> disguise.clear(type));
	}

	private static void onSetDisguise(LivingEntity entity) {
		PlayerDisguise disguise = PlayerDisguise.getOrNull(entity);
		if (disguise == null) {
			return;
		}

		PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, new PlayerDisguiseMessage(entity.getId(), Optional.of(disguise.type())));

		PlayerDisguiseBehavior.clearAttributes(entity);

		DisguiseType disguiseType = disguise.type();
		if (disguiseType.entity() != null && disguiseType.entity().applyAttributes()) {
			if (disguise.entity() instanceof LivingEntity living) {
				PlayerDisguiseBehavior.applyAttributes(entity, living);
			}
		}
	}

	private static LivingEntity getLivingEntity(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		final Entity entity = context.getSource().getEntityOrException();
		if (entity instanceof final LivingEntity living) {
			return living;
		}
		throw NOT_LIVING_ENTITY.create();
	}
}
