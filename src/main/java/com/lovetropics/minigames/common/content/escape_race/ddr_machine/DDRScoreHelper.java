package com.lovetropics.minigames.common.content.escape_race.ddr_machine;

import com.lovetropics.minigames.LoveTropics;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class DDRScoreHelper {

	public static void onGameFinished(ServerPlayer serverPlayer, JukeboxSong song, int score, int heightStreak) {
		if (song == null) {
			return;
		}
		MinecraftServer server = serverPlayer.getServer();
		Registry<JukeboxSong> registry = server.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG);
		String key = registry.getKey(song).toString();
		String objectiveName = LoveTropics.ID + ".ddr." + key.replace(":", "_");
		ServerScoreboard scoreboard = server.getScoreboard();
		Objective objective = scoreboard.getObjective(objectiveName);
		if (objective == null) {
			objective = scoreboard.addObjective(objectiveName, ObjectiveCriteria.DUMMY, Component.literal(key), ObjectiveCriteria.RenderType.INTEGER, true, null);
		}
		ScoreAccess scoreAccess = scoreboard.getOrCreatePlayerScore(serverPlayer, objective);
		if (scoreAccess.get() < score) {
			scoreAccess.set(score);
		}
	}
}
