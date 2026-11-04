package org.lovetropics.games.common.core.game.behavior.instances.tweak;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.gamerules.GameRuleMap;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GameActionEvents;

public record SetGameRulesNowBehavior(GameRuleMap rules) implements IGameBehavior {
	public static final MapCodec<SetGameRulesNowBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			GameRuleMap.CODEC.fieldOf("rules").forGetter(c -> c.rules)
	).apply(i, SetGameRulesNowBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.listen(GameActionEvents.APPLY, (context, targets) -> {
			game.allLevels().forEach(level -> level.getGameRules().setAll(rules, null));
			return true;
		});
	}
}
