package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.config.BehaviorConfig;
import com.lovetropics.minigames.common.core.game.behavior.config.ConfigList;
import com.lovetropics.minigames.common.core.game.behavior.config.ConfigType;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.util.TeamAllocator;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record AssignPlayerRolesBehavior(List<UUID> forcedParticipants) implements IGameBehavior {
	private static final Logger LOGGER = LogManager.getLogger(AssignPlayerRolesBehavior.class);
	private static final ResourceLocation CONFIG_ID = LoveTropics.location("assign_roles");
	private static final BehaviorConfig<List<UUID>> CFG_FORCED_PARTICIPANTS = BehaviorConfig.fieldOf("forced_participants", UUIDUtil.STRING_CODEC.listOf())
			.listTypeHint("", ConfigType.STRING);

	public static final MapCodec<AssignPlayerRolesBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			CFG_FORCED_PARTICIPANTS.orElse(Collections.emptyList()).forGetter(c -> c.forcedParticipants)
	).apply(i, AssignPlayerRolesBehavior::new));

	@Override
	public ConfigList getConfigurables() {
		return ConfigList.builder(CONFIG_ID)
				.with(CFG_FORCED_PARTICIPANTS, forcedParticipants)
				.build();
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		// TODO: somehow if a player is in a lobby and then leaves they can get in such a state as to join late and be joined as a participant when clicking 'play'
		events.listen(GamePhaseEvents.CREATE, () -> game.allocateRoles(createAllocator(game)));
	}

	private TeamAllocator<PlayerRole, ServerPlayer> createAllocator(IGamePhase game) {
		LOGGER.info("SELECTED ROLES: {}", game.lobby().getPlayers().getRoleSelections());
		TeamAllocator<PlayerRole, ServerPlayer> allocator = game.lobby().getPlayers().createRoleAllocator();
		allocator.setSizeForTeam(PlayerRole.PARTICIPANT, game.definition().getMaximumParticipantCount());
		LOGGER.info("TEAM SIZE: {}", game.definition().getMaximumParticipantCount());
		applyForcedParticipants(game, allocator);

		game.invoker(GamePlayerEvents.ALLOCATE_ROLES).onAllocateRoles(allocator);
		LOGGER.info("SELECTED ROLES: {}", game.lobby().getPlayers().getRoleSelections());

		return allocator;
	}

	private void applyForcedParticipants(IGamePhase game, TeamAllocator<PlayerRole, ServerPlayer> allocator) {
		LOGGER.info("FORCING PARTICIPANTS: {}", forcedParticipants);
		for (UUID uuid : forcedParticipants) {
			ServerPlayer player = game.allPlayers().getPlayerBy(uuid);
			if (player != null) {
				allocator.addPlayer(player, PlayerRole.PARTICIPANT);
			}
		}
	}
}
