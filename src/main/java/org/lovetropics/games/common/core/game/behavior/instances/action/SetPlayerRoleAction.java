package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.instances.ImmediateRespawnBehavior;
import org.lovetropics.games.common.core.game.player.PlayerRole;

import java.util.Optional;

public record SetPlayerRoleAction(
		Optional<PlayerRole> role,
		boolean respawnInPlace
) implements IGameBehavior {
	public static final MapCodec<SetPlayerRoleAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			PlayerRole.CODEC.optionalFieldOf("role").forGetter(SetPlayerRoleAction::role),
			Codec.BOOL.optionalFieldOf("respawn_in_place", false).forGetter(SetPlayerRoleAction::respawnInPlace)
	).apply(i, SetPlayerRoleAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		PlayerRole roleOrNull = role.orElse(null);
		events.applyToPlayers(game, (context, target) -> {
			if (game.getRoleFor(target) != roleOrNull) {
				target.addTag(ImmediateRespawnBehavior.TAG_RESPAWN_IN_PLACE);
				game.setPlayerRole(target, roleOrNull);
				target.setHealth(target.getMaxHealth());
				return true;
			}
			return false;
		});
	}
}
