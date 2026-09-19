package com.lovetropics.minigames.common.content.survive_the_tide.behavior;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;

public class RevealPlayersBehavior implements IGameBehavior {
	public static final MapCodec<RevealPlayersBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.INT.optionalFieldOf("players_left_required", 2).forGetter(c -> c.playersLeftRequired),
			Codec.INT.optionalFieldOf("glow_on_time", 20).forGetter(c -> c.glowOnTime),
			Codec.INT.optionalFieldOf("glow_off_time", 80).forGetter(c -> c.glowOffTime)
	).apply(i, RevealPlayersBehavior::new));

	private final int playersLeftRequired;
	private final int glowOnTime;
	private final int glowOffTime;

	private int curGlowOnTime;
	private int curGlowOffTime;

	//prevent messing with spectral arrows
	private final Set<UUID> playerToWasGlowingAlready = new ObjectOpenHashSet<>();

	public RevealPlayersBehavior(int playersLeftRequired, int glowOnTime, int glowOffTime) {
		this.playersLeftRequired = playersLeftRequired;
		this.glowOnTime = glowOnTime;
		this.glowOffTime = glowOffTime;
		curGlowOffTime = this.glowOffTime;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GamePhaseEvents.TICK, () -> {
			PlayerSet players = game.participants();
			if (players.size() > playersLeftRequired) {
				return;
			}

			if (curGlowOnTime > 0) {
				curGlowOnTime--;
				if (curGlowOnTime == 0) {
					curGlowOffTime = glowOffTime;
					for (ServerPlayer player : players) {
						//prevent unsetting glow if something else was making them glow
						if (!playerToWasGlowingAlready.contains(player.getUUID())) {
							player.setGlowingTag(false);
						}
					}
				}
			}
			if (curGlowOffTime > 0) {
				curGlowOffTime--;
				if (curGlowOffTime == 0) {
					curGlowOnTime = glowOnTime;
					playerToWasGlowingAlready.clear();
					for (ServerPlayer player : players) {
						if (player.isCurrentlyGlowing()) {
							playerToWasGlowingAlready.add(player.getUUID());
						}
						player.setGlowingTag(true);
					}
				}
			}
		});
	}
}
