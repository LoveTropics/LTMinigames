package com.lovetropics.minigames.common.core.game.behavior.instances.tweak;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GameTeamEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import javax.annotation.Nullable;
import java.util.UUID;

public record SetMaxHealthBehavior(double maxHealth, Object2DoubleMap<GameTeamKey> maxHealthByTeam) implements IGameBehavior {
	public static final MapCodec<SetMaxHealthBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.DOUBLE.optionalFieldOf("max_health", 20.0).forGetter(c -> c.maxHealth),
			MoreCodecs.object2Double(GameTeamKey.CODEC).fieldOf("max_health_by_team").orElseGet(Object2DoubleOpenHashMap::new).forGetter(c -> c.maxHealthByTeam)
	).apply(i, SetMaxHealthBehavior::new));

	private static final UUID ATTRIBUTE_ID = UUID.fromString("3e226aa5-fbcd-495e-af62-9af714b204b6");
	private static final String ATTRIBUTE_NAME = "minigame_max_health";

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GamePlayerEvents.SET_ROLE, (player, role, lastRole) -> {
			if (role == PlayerRole.PARTICIPANT) {
				applyToPlayer(game, player);
			} else if (lastRole == PlayerRole.PARTICIPANT) {
				removeFromPlayer(player);
			}
		});

		events.listen(GameTeamEvents.SET_GAME_TEAM, (player, teams, team) -> {
			removeFromPlayer(player);
			applyToPlayer(game, player);
		});

		events.listen(GamePlayerEvents.LEAVE, this::removeFromPlayer);
	}

	private void applyToPlayer(IGamePhase game, ServerPlayer player) {
		double maxHealth = getMaxHealthForPlayer(game, player);
		if (maxHealth != 20.0) {
			player.getAttribute(Attributes.MAX_HEALTH).addTransientModifier(
					new AttributeModifier(
							ATTRIBUTE_ID,
							ATTRIBUTE_NAME,
							maxHealth - 20.0,
							AttributeModifier.Operation.ADDITION
					)
			);
		}
	}

	private void removeFromPlayer(ServerPlayer player) {
		player.getAttribute(Attributes.MAX_HEALTH).removeModifier(ATTRIBUTE_ID);
	}

	private double getMaxHealthForPlayer(IGamePhase game, ServerPlayer player) {
		GameTeamKey team = getTeamOrNull(game, player);
		if (team != null) {
			return maxHealthByTeam.getOrDefault(team, maxHealth);
		}
		return maxHealth;
	}

	@Nullable
	private GameTeamKey getTeamOrNull(IGamePhase game, ServerPlayer player) {
		TeamState teamState = game.getInstanceState().getOrNull(TeamState.KEY);
		if (teamState != null) {
			return teamState.getTeamForPlayer(player);
		} else {
			return null;
		}
	}
}
