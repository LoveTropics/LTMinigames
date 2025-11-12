package com.lovetropics.minigames.common.content.escape_race.ddr_machine;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels.DdrLevel;
import com.lovetropics.minigames.common.content.escape_race.event.EscapeRaceEvents;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.impl.GameManager;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class DDRScoreHelper {

	public static void onGameFinished(ServerPlayer serverPlayer, Holder<DdrLevel> level, int score, int bestStreak) {
		MinecraftServer server = serverPlayer.getServer();
		Holder<JukeboxSong> song = level.value().track();
		String key = song.getRegisteredName();
		String objectiveName = LoveTropics.ID + ".ddr." + key.replace(":", "_");
		ServerScoreboard scoreboard = server.getScoreboard();
		Objective objective = scoreboard.getObjective(objectiveName);
		if (objective == null) {
			objective = scoreboard.addObjective(objectiveName, ObjectiveCriteria.DUMMY, song.value().description(), ObjectiveCriteria.RenderType.INTEGER, true, null);
		}
		ScoreAccess scoreAccess = scoreboard.getOrCreatePlayerScore(serverPlayer, objective);
		if (scoreAccess.get() < score) {
			scoreAccess.set(score);
		}
		IGamePhase game = GameManager.get().getGamePhaseFor(serverPlayer);
		if(game != null){
			game.invoker(EscapeRaceEvents.DDR_LEVEL_COMPLETED)
					.onComplete(serverPlayer, level, score, bestStreak);
		}
	}
}
