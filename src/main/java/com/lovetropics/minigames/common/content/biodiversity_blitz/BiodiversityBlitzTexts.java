package com.lovetropics.minigames.common.content.biodiversity_blitz;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.biodiversity_blitz.entity.BbMobSpawner;
import com.lovetropics.minigames.common.core.game.util.TranslationCollector;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.function.BiConsumer;

public final class BiodiversityBlitzTexts {
	private static final TranslationCollector KEYS = new TranslationCollector(LoveTropics.ID + ".minigame.biodiversity_blitz.");

	public static final Component WAVE_WARNING = KEYS.add("wave_warning", "A wave is coming soon! Make sure your plant defenses are ready!");
	public static final Component WAVE_INCOMING = KEYS.add("wave_incoming", "Wave Incoming!");
	public static final TranslationCollector.Fun1 WAVE_NUMBER = KEYS.add1("wave_number", "Wave %s");
	public static final Component TRADING = KEYS.add("trading", "Trading");
	private static final TranslationCollector.Fun1 DEATH_DECREASE = KEYS.add1("death_decrease", "...and lost %s biodiversity points!");
	public static final Component DEATH_TITLE = KEYS.add("death_title", "YOU DIED");
	private static final TranslationCollector.Fun1 CURRENCY_ADDITION = KEYS.add1("currency_addition", "You gained %s biodiversity points!");
	public static final Component CAN_ONLY_PLACE_PLANTS = KEYS.add("can_only_place_plants", "You can only place plants you got from the shop!");
	public static final Component PLANT_CANNOT_FIT = KEYS.add("plant_cannot_fit", "This plant cannot fit here!");
	public static final Component PLANT_CANNOT_BE_PLACED_IN_BIOME = KEYS.add("plant_cannot_be_placed_in_biome", "This plant cannot be placed in this biome!");
	public static final Component NOT_YOUR_PLOT = KEYS.add("not_your_plot", "This is not your plot, you cannot edit here!");
	public static final Component SHIFT_FOR_MORE_INFORMATION = KEYS.add("shift_for_more_information", "Hold Shift for more information");
	public static final Component SCOREBOARD_TITLE = KEYS.add("scoreboard.title", "Biodiversity Blitz").withStyle(ChatFormatting.GREEN);
	public static final TranslationCollector.Fun1 SCOREBOARD_POINTS = KEYS.add1("scoreboard.points", "%s Points!");
	public static final TranslationCollector.Fun1 SCOREBOARD_POINTS_INCREMENT = KEYS.add1("scoreboard.points.increment", "+ %s per drop");
	private static final TranslationCollector.Fun2 SEND_MOBS_TOOLTIP = KEYS.add2("send_mobs.tooltip", "%sx %s");
	public static final TranslationCollector.Fun2 SENT_MOBS_MESSAGE = KEYS.add2("sent_mobs.message", "%s has sent you a few mobs! Next wave you will encounter the following mobs: %s");

	public static final Component SHOP = KEYS.add("shop", "Shop");
	public static final Component PLANT_SHOP = KEYS.add("plant_shop", "Plant Shop");
	public static final Component MOB_SHOP = KEYS.add("mob_shop", "Mob Shop");

	public static final Component WAVE_SPAWNED = KEYS.add("wave_spawned", "A wave has spawned! Your plant defense may not yet be self-sufficient, use your sword to defend your plot!");
	public static final Component[] TIPS = {
			KEYS.add("tips.diversity", "Make sure to have a plot with many different plant types to gain more Biodiversity Points!"),
			KEYS.add("tips.diffuse", "The Commelina Diffusa can put out the Crocosmia's fires. Be careful of where you plant them!"),
			KEYS.add("tips.pianguas", "Mangrove trees produce pianguas in mud that can be harvested via right click to sell at the store!"),
			KEYS.add("tips.creeper", "Beware the creeper. The legends say that its explosion can level even the sturdiest of trees..."),
			KEYS.add("tips.wave_difficulty", "The waves will get harder as time goes on, so be quick!"),
			KEYS.add("tips.visit", "You can visit other players' plots to learn from how they've set up their plot!"),
			KEYS.add("tips.pumpkins", "Mobs loooove pumpkins. Place them in strategic places to create elaborate traps!"),
			KEYS.add("tips.monoculture", "Be careful of having too much of the same plant, monocultures can hurt your point production!"),
			KEYS.add("tips.crops", "Crops are weaker than other plants, so put them in places where mobs can't get to them!"),
			KEYS.add("tips.oak", "Rumours say that if you're patient enough, oak trees will drop golden apples. Could it possibly be true?"),
			KEYS.add("tips.tilling", "If you change your mind about tilling a piece of land, simply right click it with your hoe to make it into grass again!")
	};

	static {
		KEYS.add("teams.intro1", "Welcome to Biodiversity Blitz!");
		KEYS.add("teams.intro2", "As the professional conservationist that you surely are, you must turn this bare plot into a thriving ecosystem.");
		KEYS.add("teams.intro3", "\nYou can buy plants and items from the shop using your 'Biodiversity points' and place them in your plot!");
		KEYS.add("teams.intro4", "\nThese plants may provide food, defense, or even attack the invasive species. Every plant you place improves the number of biodiversity points received per turn.");
		KEYS.add("teams.intro5", "\nInvasive species will be spawning soon! But if you construct an ecosystem that is resilient enough, you may be able to survive.");
		KEYS.add("teams.intro6", "\nMake sure to have a large variety of different plants in your plot- diversity in an ecosystem only makes you stronger!");
		KEYS.add("teams.intro7", "\nYou win the game once your team accumulates the target amount of Biodiversity Points - you can check your progress in the top-left of your screen!");
		KEYS.add("teams.intro8", "\nGood luck!");

		KEYS.add("plant.provides_currency", "Provides currency.");
		KEYS.add("plant.tradeable_points", "Can be traded at the Shop for Biodiversity Points.");
		KEYS.add("plant.nutrition", "Good source of nutrition!");
		KEYS.add("plant.staple", "A staple crop for many.");
		KEYS.add("plant.beetroot.tooltip", "Makes great soup!");
		KEYS.add("plant.carrot.tooltip.extra", "Grows into a Carrot Crop after a few growth cycles.");
		KEYS.add("plant.potato.tooltip.extra1", "Grows into a Potato Crop after a few growth cycles.");
		KEYS.add("plant.potato.tooltip.extra2", "Can be traded at the Shop for Baked Potatoes or Biodiversity Points.");
		KEYS.add("plant.wheat.tooltip.extra1", "Grows into a Wheat Crop after a few growth cycles.");
		KEYS.add("plant.wheat.tooltip.extra2", "Can be traded at the Shop for Bread or Biodiversity Points.");
		KEYS.add("plant.canna.tooltip1", "Shoots lightning at mobs and sets them on fire.");
		KEYS.add("plant.canna.tooltip2", "Charges creepers.");
		KEYS.add("plant.commelina_diffusa.tooltip", "Damages nearby mobs with water. Puts out fires.");
		KEYS.add("plant.commelina_diffusa.tooltip.extra1", "When placed, any mobs in a small radius nearby will get hit by a stream of water, damaging them.");
		KEYS.add("plant.commelina_diffusa.tooltip.extra2", "Useful as a front-line defense as it actively tries to attack mobs. Be careful where you put it, as it can put out the Crocosmia's fires.");
		KEYS.add("plant.crocosmia.tooltip", "Lights nearby mobs on fire.");
		KEYS.add("plant.crocosmia.tooltip.extra1", "When placed, any mobs in a small radius nearby will get hit by a jet of flames, setting them on fire.");
		KEYS.add("plant.crocosmia.tooltip.extra2", "Useful as a front-line defense as it actively tries to attack mobs. Be careful where you put it, as the Commelina Diffusa's water can put it out.");
		KEYS.add("plant.crocosmia.tooltip.extra3", "Be careful! Enemies set on fire will set you on fire if they attack you.");
		KEYS.add("plant.fern.tooltip", "Greatly slows down mobs walking through it.");
		KEYS.add("plant.fern.tooltip.extra1", "When mobs walk through Ferns, they will get the Slowness effect.");
		KEYS.add("plant.fern.tooltip.extra2", "Mobs will not target this block: They will walk right through.");
		KEYS.add("plant.grass.tooltip", "Greatly slows down mobs walking through it.");
		KEYS.add("plant.grass.tooltip.extra1", "When mobs walk through Grass, they will get the Slowness effect.");
		KEYS.add("plant.grass.tooltip.extra2", "Mobs will not target this block: They will walk right through.");
		KEYS.add("plant.iris.tooltip", "Applies a Glowing effect to all nearby mobs.");
		KEYS.add("plant.iris.tooltip.extra1", "Gives all nearby mobs on the field the Glowing effect.");
		KEYS.add("plant.iris.tooltip.extra2", "Useful for detecting where the mobs are when hidden behind trees.");
		KEYS.add("plant.bromeliad.tooltip", "Slowly kills mobs that walk nearby.");
		KEYS.add("plant.jack_o_lantern.tooltip", "Scares nearby mobs away! Must be reset after triggering.");
		KEYS.add("plant.jack_o_lantern.tooltip.extra1", "When mobs walk towards the Jack o' Lantern, they get scared and avoid the area.");
		KEYS.add("plant.jack_o_lantern.tooltip.extra2", "Jack o' Lanterns need to be reset after use and do not contribute towards your biodiversity score.");
		KEYS.add("plant.magic_mushroom.tooltip", "Creates extra points when mobs die near it.");
		KEYS.add("plant.melon.tooltip", "Explodes when monsters come too close.");
		KEYS.add("plant.melon.tooltip.extra1", "When mobs walk close to the melon, the melon explodes.");
		KEYS.add("plant.melon.tooltip.extra2", "This explosion does not affect plants or the player: only mobs.");
		KEYS.add("plant.melon.tooltip.extra3", "Melons do not contribute towards your biodiversity score.");
		KEYS.add("plant.pumpkin.tooltip", "Attracts mobs, but eventually breaks.");
		KEYS.add("plant.pumpkin.tooltip.extra1", "When placed, all mobs will path directly towards the Pumpkin and gather around it.");
		KEYS.add("plant.pumpkin.tooltip.extra2", "This causes the Pumpkin to take damage and eventually break, but it allows you to make interesting traps by forcing monsters towards a certain area.");
		KEYS.add("plant.pumpkin.tooltip.extra3", "Pumpkins do not contribute towards your biodiversity score.");
		KEYS.add("plant.sweet_berry_bush.tooltip", "Grows into a plant that harms monsters when walked over.");
		KEYS.add("plant.sweet_berry_bush.tooltip.extra1", "When placed, grows into a Berry Bush that damages mobs that walk through it.");
		KEYS.add("plant.sweet_berry_bush.tooltip.extra2", "Mobs will not target this block: they will walk right through.");
		KEYS.add("plant.sweet_berry_bush.tooltip.extra3", "Rarely, Berries will grow, which can then be sold at the shop for Biodiversity Points.");
		KEYS.add("plant.wither_rose.tooltip", "Applies the Wither effect to mobs touching it.");
		KEYS.add("plant.wither_rose.tooltip.extra1", "When walked through, mobs will get the Wither effect. That includes you!");
		KEYS.add("plant.wither_rose.tooltip.extra2", "Mobs will not target this block: they will walk right through.");
	}

	public static void collectTranslations(BiConsumer<String, String> consumer) {
		KEYS.forEach(consumer);

		consumer.accept(LoveTropics.ID + ".minigame.biodiversity_blitz", "Biodiversity Blitz");
		consumer.accept(LoveTropics.ID + ".minigame.biodiversity_blitz_teams", "Biodiversity Blitz (Teams)");
		for (BbMobSpawner.BbEntityTypes type : BbMobSpawner.BbEntityTypes.values()) {
			consumer.accept(type.getTranslationKey(), type.getEnglishName());
		}
	}

	public static MutableComponent deathDecrease(int count) {
		return DEATH_DECREASE.apply(count);
	}

	public static MutableComponent currencyAddition(int amount) {
		Component amountText = Component.literal(String.valueOf(amount))
				.withStyle(amount > 0 ? ChatFormatting.AQUA : ChatFormatting.RED);
		return CURRENCY_ADDITION.apply(amountText);
	}

	public static MutableComponent sendMobsTooltip(BbMobSpawner.BbEntityTypes entity, int count) {
		return SEND_MOBS_TOOLTIP.apply(count, entity.getName().withStyle(ChatFormatting.GOLD));
	}
}
