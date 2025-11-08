package com.lovetropics.minigames.common.content.qottott;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.util.TranslationCollector;

public class QottottTexts {
	public static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".minigame.qottott.");

	static {
		KEYS.add("sidebar.title", "Qottott");
		KEYS.add("instruction", "Pick up coins \uE040!");
		KEYS.add("sidebar.top_players", "Top Players:");
		KEYS.add("waiting", "Pick a kit before the game starts!");
		KEYS.add("intro", "Pick a kit! The game will begin soon!");
		KEYS.add("started", "The game has begun - walk into the portal to enter the map!");
		KEYS.add("kit.picked", "You have selected: %s");
		KEYS.add("kit.acrobat", "Acrobat");
		KEYS.add("kit.assassin", "Assassin");
		KEYS.add("kit.glass_cannon", "Glass Cannon");
		KEYS.add("kit.opportunist", "Opportunist");
		KEYS.add("kit.fighter", "Fighter");
		KEYS.add("kit.brawler", "Brawler");
		KEYS.add("power_up.generic", "Power-up for %seconds% seconds");
		KEYS.add("spawn.power_up", "%item% has spawned!");
		KEYS.add("power_up.title", "Power-up!");
		KEYS.add("power_up.subtitle", "%s + 2x \uE040");
		KEYS.add("kill.title", "+2 \uE040");
		KEYS.add("kill.subtitle", "☠ %killed%");
		KEYS.add("speed_power_up", "Speed Boost Power-up");
		KEYS.add("speed_power_up.subtitle", "Speed Boost");
		KEYS.add("knockback_resistance_power_up", "Knockback Resistance Power-up");
		KEYS.add("knockback_resistance_power_up.subtitle", "Knockback Resistance");
		KEYS.add("resistance_power_up", "Resistance Power-up");
		KEYS.add("resistance_power_up.subtitle", "Damage Resistance");
		KEYS.add("health_power_up", "Health Boost Power-up");
		KEYS.add("health_power_up.subtitle", "Health Boost");
		KEYS.add("pickup_priority_power_up", "Item Pickup Priority Power-up");
		KEYS.add("pickup_priority_power_up.subtitle", "Pick up items faster");
		KEYS.add("cash_crab", "Cash Crab");
		KEYS.add("cash_crab.description", "Hits drop 20% of the target's \uE040");
		KEYS.add("cash_crab.killed.title", "Oh no!");
		KEYS.add("cash_crab.killed.subtitle", "%killer% stole 20% of your \uE040!");
		KEYS.add("cash_crab.leaky_pockets.title", "Cash Crab!");
		KEYS.add("cash_crab.leaky_pockets.subtitle", "Your pockets are leaking!");
	}
}
