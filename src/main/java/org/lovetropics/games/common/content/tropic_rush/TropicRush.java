package org.lovetropics.games.common.content.tropic_rush;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.content.tropic_rush.behavior.FinishBehavior;
import org.lovetropics.games.common.content.tropic_rush.behavior.PlayerWorldsBehavior;
import org.lovetropics.games.common.content.tropic_rush.behavior.SpyingBehavior;
import org.lovetropics.games.common.content.tropic_rush.modifier.AttributePerLevelBehavior;
import org.lovetropics.games.common.content.tropic_rush.modifier.ChangeModifierAction;
import org.lovetropics.games.common.content.tropic_rush.modifier.EnderPearlDeviationBehavior;
import org.lovetropics.games.common.content.tropic_rush.modifier.ModifiersBehavior;
import org.lovetropics.games.common.content.tropic_rush.modifier.RevealModifierAction;
import org.lovetropics.games.common.content.tropic_rush.modifier.ScrambleDebugScreenBehavior;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

/// Competitive Minecraft speedrunning mini-game
public class TropicRush {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static void init() {
		REGISTRATE.registerBehavior("tropic_rush/player_worlds", PlayerWorldsBehavior.CODEC);
		REGISTRATE.registerBehavior("tropic_rush/finish", FinishBehavior.CODEC);
		REGISTRATE.registerBehavior("tropic_rush/spying", SpyingBehavior.CODEC);

		REGISTRATE.registerBehavior("tropic_rush/modifiers", ModifiersBehavior.CODEC);
		REGISTRATE.registerBehavior("tropic_rush/change_modifier", ChangeModifierAction.CODEC);
		REGISTRATE.registerBehavior("tropic_rush/reveal_modifier", RevealModifierAction.CODEC);
		REGISTRATE.registerBehavior("tropic_rush/ender_pearl_deviation", EnderPearlDeviationBehavior.CODEC);
		REGISTRATE.registerBehavior("tropic_rush/attribute_per_level", AttributePerLevelBehavior.CODEC);
		REGISTRATE.registerBehavior("tropic_rush/scramble_debug_screen", ScrambleDebugScreenBehavior.CODEC);
	}
}
