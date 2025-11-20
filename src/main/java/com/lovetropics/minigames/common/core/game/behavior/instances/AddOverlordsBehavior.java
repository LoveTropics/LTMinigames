package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.lib.permission.PermissionsApi;
import com.lovetropics.lib.permission.role.Role;
import com.lovetropics.lib.permission.role.RoleReader;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.state.Overlords;
import com.lovetropics.minigames.common.core.game.state.statistics.PlayerKey;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record AddOverlordsBehavior(
		List<String> roles,
		Set<UUID> playerIds
) implements IGameBehavior {
	private static final Logger LOGGER = LogUtils.getLogger();

	public static final MapCodec<AddOverlordsBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.listOf().optionalFieldOf("roles", List.of()).forGetter(AddOverlordsBehavior::roles),
			UUIDUtil.CODEC_SET.optionalFieldOf("players", Set.of()).forGetter(AddOverlordsBehavior::playerIds)
	).apply(i, AddOverlordsBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		List<Role> roles = this.roles.stream().map(id -> {
			Role role = PermissionsApi.provider().get(id);
			if (role == null) {
				if (FMLEnvironment.production) {
					throw new GameException(Component.literal("No role with id: '" + id + "'"));
				} else {
					LOGGER.warn("No role with id: '{}'", id);
				}
			}
			return role;
		}).filter(Objects::nonNull).toList();

		Overlords overlords = Overlords.get(game);

		// TODO: Can we update SELECT_ROLE_ON_JOIN to also be invoked on game setup?
		events.listen(GamePlayerEvents.ALLOCATE_ROLES, allocator -> {
			for (PlayerKey player : allocator.getKnownPlayers()) {
				if (shouldBeOverlord(roles, player)) {
					allocator.addPlayer(player, PlayerRole.SPECTATOR);
				}
			}
		});
		events.listen(GamePlayerEvents.SELECT_ROLE_ON_JOIN, (player, requestedRole) -> {
			if (shouldBeOverlord(roles, player)) {
				return PlayerRole.SPECTATOR;
			}
			return requestedRole;
		});
		events.listen(GamePlayerEvents.ADD, player -> {
			if (shouldBeOverlord(roles, PlayerKey.from(player))) {
				overlords.add(player);
			}
		});
		events.listen(GamePlayerEvents.SET_ROLE, (player, role, lastRole) -> {
			// This is stupid, but we need to run after the normal set_game_types
			if (overlords.contains(player)) {
				onAddOverlord(game, player);
			}
		});
		events.listen(GamePlayerEvents.LEAVE, overlords::remove);

		events.listen(GamePhaseEvents.REGISTER_COMMANDS, (commands, buildContext) -> {
			commands.register(Commands.literal("overlord")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.then(Commands.literal("add")
							.then(Commands.argument("players", EntityArgument.players())
									.executes(context -> {
										EntityArgument.getPlayers(context, "players").forEach(player -> {
											overlords.add(player);
											onAddOverlord(game, player);
										});
										return 1;
									})
							)
					)
					.then(Commands.literal("remove")
							.then(Commands.argument("players", EntityArgument.players())
									.executes(context -> {
										EntityArgument.getPlayers(context, "players").forEach(player -> {
											if (overlords.remove(player)) {
												onRemoveOverlord(game, player);
											}
										});
										return 1;
									})
							)
					)
			);
		});
	}

	private void onAddOverlord(IGamePhase game, ServerPlayer player) {
		game.setPlayerRole(player, PlayerRole.SPECTATOR);
		player.setGameMode(GameType.CREATIVE);
	}

	private void onRemoveOverlord(IGamePhase game, ServerPlayer player) {
		// TODO: Welp. We need some better way of managing this.
		game.setPlayerRole(player, null);
		game.setPlayerRole(player, PlayerRole.SPECTATOR);
	}

	private boolean shouldBeOverlord(List<Role> roles, PlayerKey player) {
		RoleReader roleReader = PermissionsApi.lookup().byPlayerId(player.id());
		return playerIds.contains(player.id()) || roles.stream().anyMatch(roleReader::has);
	}
}
