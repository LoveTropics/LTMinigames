package org.lovetropics.games.common.core.game.state.statistics;

import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.state.team.GameTeamKey;
import org.lovetropics.games.common.core.game.state.team.TeamState;

import java.util.UUID;

public final class PlayerKey implements StatisticHolder {
	// TODO: We should probably update this format :(
	public static final Codec<PlayerKey> UUID_CODEC = UUIDUtil.STRING_CODEC.xmap(
			uuid -> new PlayerKey(new NameAndId(uuid, "Unknown")),
			PlayerKey::id
	);

	private final NameAndId nameAndId;

	private PlayerKey(NameAndId nameAndId) {
		this.nameAndId = nameAndId;
	}

	public static PlayerKey from(Player player) {
		return new PlayerKey(player.nameAndId());
	}

	public static PlayerKey from(NameAndId nameAndId) {
		return new PlayerKey(nameAndId);
	}

	public UUID id() {
		return nameAndId.id();
	}

	public String name() {
		return nameAndId.name();
	}

	public NameAndId nameAndId() {
		return nameAndId;
	}

	@Override
	public String toString() {
		return nameAndId.name();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj instanceof PlayerKey key) {
			return nameAndId.id().equals(key.nameAndId.id());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return nameAndId.id().hashCode();
	}

	public boolean matches(Entity entity) {
		return entity instanceof ServerPlayer && entity.getUUID().equals(nameAndId.id());
	}

	@Override
	public Component getName(IGamePhase game) {
		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		GameTeamKey team = teams != null ? teams.getTeamForPlayer(id()) : null;
		if (team != null) {
			return Component.literal(name()).withColor(teams.getTeamOrThrow(team).textColor());
		}
		return Component.literal(name());
	}

	@Override
	public StatisticsMap getOwnStatistics(GameStatistics statistics) {
		return statistics.forPlayer(this);
	}
}
