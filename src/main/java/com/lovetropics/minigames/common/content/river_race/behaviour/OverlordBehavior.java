package com.lovetropics.minigames.common.content.river_race.behaviour;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;

public class OverlordBehavior implements IGameBehavior {
	// Yes i know this is janky
	public static final MapCodec<OverlordBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			GameActionList.PLAYER_CODEC.optionalFieldOf("powers", GameActionList.EMPTY_PLAYER).forGetter(c -> c.powers)
	).apply(i, OverlordBehavior::new));

	private final GameActionList<ServerPlayer> powers;

	public OverlordBehavior(GameActionList<ServerPlayer> powers) {
		this.powers = powers;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
        /* THIS IS TEMPORARY LOL CHILL ANYBODY WHO READS THIS
        events.listen(GamePlayerEvents.CHAT, (player, message) -> {


            final String contents = message.unsignedContent().getString();
            for (final String power : powers) {
                if (power.contains(contents)) {
                    if (power.equals("freeze")) {
                        ServerLevel level = game.level();

                        for (ServerPlayer sp : level.players()) {
                            sp.hurt(sp.damageSources().freeze(), 1);
                        }
                        player.sendSystemMessage(Component.
                                literal("OVERLORD HAS HURT YOU!!")
                                .withStyle(ChatFormatting.GREEN));
                    }
                }
            }
            return false;
        });
        */
	}
}
