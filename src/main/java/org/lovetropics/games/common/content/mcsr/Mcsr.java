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
import org.lovetropics.games.common.util.registry.GameBehaviorEntry;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

/// Competitive Minecraft speedrunning mini-game
public class Mcsr {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameBehaviorEntry<PlayerWorldsBehavior> PLAYER_WORLDS = REGISTRATE.object("mcsr/player_worlds").behavior(PlayerWorldsBehavior.CODEC).register();
	public static final GameBehaviorEntry<FinishBehavior> FINISH = REGISTRATE.object("mcsr/finish").behavior(FinishBehavior.CODEC).register();
	public static final GameBehaviorEntry<ShopBehavior> SHOP = REGISTRATE.object("mcsr/shop").behavior(ShopBehavior.CODEC).register();

	public static final GameBehaviorEntry<ModifiersBehavior> MODIFIERS = REGISTRATE.object("mcsr/modifiers").behavior(ModifiersBehavior.CODEC).register();
	public static final GameBehaviorEntry<ChangeModifierAction> CHANGE_MODIFIER = REGISTRATE.object("mcsr/change_modifier").behavior(ChangeModifierAction.CODEC).register();
	public static final GameBehaviorEntry<RevealModifierAction> REVEAL_MODIFIER = REGISTRATE.object("mcsr/reveal_modifier").behavior(RevealModifierAction.CODEC).register();
	public static final GameBehaviorEntry<EnderPearlDeviationBehavior> ENDER_PEARL_DEVIATION = REGISTRATE.object("mcsr/ender_pearl_deviation").behavior(EnderPearlDeviationBehavior.CODEC).register();
	public static final GameBehaviorEntry<AttributePerLevelBehavior> ATTRIBUTE_PER_LEVEL = REGISTRATE.object("mcsr/attribute_per_level").behavior(AttributePerLevelBehavior.CODEC).register();
	public static final GameBehaviorEntry<ScrambleDebugScreenBehavior> SCRAMBLE_DEBUG_SCREEN = REGISTRATE.object("mcsr/scramble_debug_screen").behavior(ScrambleDebugScreenBehavior.CODEC).register();

	public static void init() {
	}
}
