package com.lovetropics.minigames.common.content.box_hunt;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.BlockPredicate;

import java.util.Optional;

public record HitBlockCausesDamageBehaviour(
		Optional<BlockPredicate> blockPredicate,
		Optional<GameTeamKey> team,
		float damage
) implements IGameBehavior {

	public static final MapCodec<HitBlockCausesDamageBehaviour> CODEC =
			RecordCodecBuilder.mapCodec(inst ->
					inst.group(
							BlockPredicate.CODEC.optionalFieldOf("block_predicate").forGetter(HitBlockCausesDamageBehaviour::blockPredicate),
							GameTeamKey.CODEC.optionalFieldOf("team").forGetter(HitBlockCausesDamageBehaviour::team),
							Codec.FLOAT.fieldOf("damage").forGetter(HitBlockCausesDamageBehaviour::damage)
					).apply(inst, HitBlockCausesDamageBehaviour::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		TeamState teamState = game.instanceState().getOrNull(TeamState.KEY);
		events.listen(GamePlayerEvents.LEFT_CLICK_BLOCK, (player, level, pos) -> {
			if(team.isPresent()){
				if(teamState != null && teamState.isOnTeam(player, team.get())){
					if(blockPredicate.isPresent()) {
						if(blockPredicate.get().matches(level, pos)){
							player.hurt(player.damageSources().generic(), damage);
						}
					} else {
						player.hurt(player.damageSources().generic(), damage);
					}
				}
			} else {
				player.hurt(player.damageSources().generic(), damage);
			}
		});
	}

}
