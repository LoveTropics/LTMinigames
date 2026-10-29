package org.lovetropics.games.common.content.mcsr;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.content.mcsr.behavior.FinishBehavior;
import org.lovetropics.games.common.content.mcsr.behavior.PlayerWorldsBehavior;
import org.lovetropics.games.common.content.mcsr.modifier.AttributePerLevelBehavior;
import org.lovetropics.games.common.content.mcsr.modifier.ChangeModifierAction;
import org.lovetropics.games.common.content.mcsr.modifier.EnderPearlDeviationBehavior;
import org.lovetropics.games.common.content.mcsr.modifier.ModifiersBehavior;
import org.lovetropics.games.common.content.mcsr.modifier.RevealModifierAction;
import org.lovetropics.games.common.content.mcsr.modifier.ScrambleDebugScreenBehavior;
import org.lovetropics.games.common.content.mcsr.shop.ShopBehavior;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

/// Competitive Minecraft speedrunning mini-game
public class Mcsr {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static void init() {
		REGISTRATE.registerBehavior("mcsr/player_worlds", PlayerWorldsBehavior.CODEC);
		REGISTRATE.registerBehavior("mcsr/finish", FinishBehavior.CODEC);
		REGISTRATE.registerBehavior("mcsr/shop", ShopBehavior.CODEC);

		REGISTRATE.registerBehavior("mcsr/modifiers", ModifiersBehavior.CODEC);
		REGISTRATE.registerBehavior("mcsr/change_modifier", ChangeModifierAction.CODEC);
		REGISTRATE.registerBehavior("mcsr/reveal_modifier", RevealModifierAction.CODEC);
		REGISTRATE.registerBehavior("mcsr/ender_pearl_deviation", EnderPearlDeviationBehavior.CODEC);
		REGISTRATE.registerBehavior("mcsr/attribute_per_level", AttributePerLevelBehavior.CODEC);
		REGISTRATE.registerBehavior("mcsr/scramble_debug_screen", ScrambleDebugScreenBehavior.CODEC);
	}
}
