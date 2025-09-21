package com.lovetropics.minigames.common.content.survive_the_tide;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.util.TranslationCollector;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class SurviveTheTideTexts {
	public static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".minigame.survive_the_tide.");

	public static final TranslationCollector.Fun1 SIDEBAR_WEATHER = KEYS.add1("sidebar.weather", "Weather: %s");
	public static final Component SIDEBAR_PVP_DISABLED = KEYS.add("sidebar.pvp_disabled", "PVP disabled").withStyle(ChatFormatting.YELLOW);
	public static final Component SIDEBAR_TIDE_RISING = KEYS.add("sidebar.tide_rising", "The tide is rising!").withStyle(ChatFormatting.RED);
	public static final Component SIDEBAR_ICEBERGS_FORMING = KEYS.add("sidebar.icebergs_forming", "Icebergs are forming!").withStyle(ChatFormatting.AQUA);
	public static final Component SIDEBAR_EXPLOSIVE_STORM = KEYS.add("sidebar.explosive_storm", "Explosive storm closing!").withStyle(ChatFormatting.RED);
	public static final TranslationCollector.Fun2 SIDEBAR_PLAYER_COUNT = KEYS.add2("sidebar.player_count", "%s/%s players").withStyle(ChatFormatting.GRAY);

	public static final Component ACID_REPELLENT_UMBRELLA_TOOLTIP = KEYS.add("acid_repellent_umbrella.tooltip", "Prevents acid rain from harming you.\n\nActive when held in main or offhand.").withStyle(ChatFormatting.AQUA);
	public static final Component PADDLE_TOOLTIP = KEYS.add("paddle.tooltip", "This might come in handy!").withStyle(ChatFormatting.AQUA);
	public static final Component SUPER_SUNSCREEN_TOOLTIP = KEYS.add("super_sunscreen.tooltip", "Prevents heatwaves from slowing you down.\n\nActive when held in main or offhand.").withStyle(ChatFormatting.AQUA);

	public static final Component BAR_PVP_DISABLED = KEYS.add("bar.pvp_disabled", "Grace Period: PVP Disabled");
	public static final Component BAR_COLLECT_LOOT = KEYS.add("bar.collect_loot", "Collect Loot Before the Tide Rises");
	public static final Component BAR_TIDES_RISING = KEYS.add("bar.tides_rising", "Tides Rising");
	public static final Component BAR_FLASH_FLOOD = KEYS.add("bar.flash_flood", "Flash Flood");
	public static final Component BAR_FREEZING_OVER = KEYS.add("bar.freezing_over", "Freezing Over");
	public static final Component BAR_EXPLOSIVE_STORM = KEYS.add("bar.explosive_storm", "Explosive Storm");

	public static final Component RELEASE_PLAYERS_TOAST = KEYS.add("toast.release_players", "The tide will soon begin to rise, and PVP will be enabled! Collect resources before the time runs out!");
	public static final Component ICEBERGS_START_TOAST = KEYS.add("toast.icebergs_start", "The map is freezing over!");
	public static final Component EXPLOSIVE_STORM_START_TOAST = KEYS.add("toast.explosive_storm_start", "THE EXPLOSIVE STORM HAS STARTED CLOSING IN! Get to the center!");
	public static final Component LOOT_DROP_TOAST = KEYS.add("toast.loot_drop", "A loot drop will arrive in 1 minute! Find the nearest beacon beam!");

	public static final Component ACID_RAIN_TOAST = KEYS.add("toast.acid_rain", "WEATHER REPORT:\nAcid Rain is falling!\nFind shelter, or make sure to carry an Acid Repellent Umbrella!");
	public static final Component HAIL_TOAST = KEYS.add("toast.hail", "WEATHER REPORT:\nHail is falling!\nFind shelter, or make sure to carry an Acid Repellent Umbrella!");
	public static final Component HEAT_WAVE_TOAST = KEYS.add("toast.heat_wave", "WEATHER REPORT:\nA Heat Wave is passing!\nStay inside, or make sure to equip Super Sunscreen!");
	public static final Component SANDSTORM_TOAST = KEYS.add("toast.sandstorm", "WEATHER REPORT:\nA Sandstorm is passing!\nFind shelter!");
	public static final Component SNOWSTORM_TOAST = KEYS.add("toast.snowstorm", "WEATHER REPORT:\nA Snowstorm is passing!\nFind shelter!");
	public static final Component FLASH_FLOOD_TOAST = KEYS.add("toast.flash_flood", "WEATHER REPORT:\nSeek higher ground! Heavy rains are falling!");

	static {
		KEYS.add("eliminated", "☠ %message%. They are eliminated!");
		KEYS.add("outro.ffa1", "Through the rising sea levels, the volatile and chaotic weather, and the struggle to survive, one player remains: %winner%.");
		KEYS.add("outro.ffa2", "\nThose who have fallen have been swept away by the encroaching tides that engulf countless landmasses in this dire future.");
		KEYS.add("outro.ffa3", "\nThe lone survivor of this city, %winner%, has won - but at what cost? The world is not what it once was, and they must survive in this new apocalyptic land.");
		KEYS.add("outro.ffa4", "\nWhat would you do different next time? Together, we could stop this from becoming our future.");
		KEYS.add("outro.ffa5", "The game will close in 10 seconds...");
		KEYS.add("outro.teams1", "Through the rising sea levels, the volatile and chaotic weather, and the struggle to survive, one team remains: %winner%.");
		KEYS.add("outro.teams2", "\nThose who have fallen have been swept away by the encroaching tides that engulf countless landmasses in this dire future.");
		KEYS.add("outro.teams3", "\nThe remaining survivors of this island, have won - but at what cost? The world is not what it once was, and they must survive in this new apocalyptic land.");
		KEYS.add("outro.teams4", "\nWhat would you do different next time? Together, we could stop this from becoming our future.");
		KEYS.add("outro.teams5", "The game will close in 10 seconds...");
		KEYS.add("camping_warning", "You should not be here!");
		KEYS.add("lightning_storm", "Lightning Storm!");
		KEYS.add("lightning_storm.subtitle", "Look out!");
		KEYS.add("zombie_invasion", "Zombie Invasion");
		KEYS.add("zombie_invasion.subtitle", "Look out!");
		KEYS.add("meteor_shower", "Meteor Shower!");
		KEYS.add("meteor_shower.subtitle", "Look out!");

		KEYS.add("stt4.intro1", "ATTENTION ALL REMAINING UN SCIENCE PERSONNEL");
		KEYS.add("stt4.intro2", "The final evacuation shuttle is departing in 15 minutes");
		KEYS.add("stt4.intro3", "Meteorological sensors indicate incoming hostile rainfall patterns.");
		KEYS.add("stt4.intro4", "Seek higher ground.");
	}
}
