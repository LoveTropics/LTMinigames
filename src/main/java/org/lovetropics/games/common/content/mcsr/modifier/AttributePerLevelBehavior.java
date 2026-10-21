package org.lovetropics.games.common.content.mcsr.modifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;

/// Changes an attribute of the participants by a given amount for each level of a modifier,
/// e.g. stronger gravity or slower mining the stronger the modifier is
public record AttributePerLevelBehavior(String modifier, Holder<Attribute> attribute, double amountPerLevel, AttributeModifier.Operation operation) implements IGameBehavior {
	public static final MapCodec<AttributePerLevelBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("modifier").forGetter(AttributePerLevelBehavior::modifier),
			Attribute.CODEC.fieldOf("attribute").forGetter(AttributePerLevelBehavior::attribute),
			Codec.DOUBLE.fieldOf("amount_per_level").forGetter(AttributePerLevelBehavior::amountPerLevel),
			AttributeModifier.Operation.CODEC.optionalFieldOf("operation", AttributeModifier.Operation.ADD_VALUE).forGetter(AttributePerLevelBehavior::operation)
	).apply(i, AttributePerLevelBehavior::new));

	private static final int UPDATE_INTERVAL = 10;

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		GameModifiers modifiers = GameModifiers.getOrThrow(game, modifier);
		Identifier id = LoveTropics.id("mcsr_modifier/" + modifier);

		// Catches level changes, players who just arrived and respawned players, who don't keep transient modifiers
		events.listen(GamePhaseEvents.TICK, () -> {
			if (game.ticks() % UPDATE_INTERVAL == 0) {
				int level = modifiers.level(modifier);
				for (ServerPlayer player : game.participants()) {
					update(player, id, level);
				}
			}
		});
		events.listen(GamePlayerEvents.REMOVE, player -> remove(player, id));
		events.listen(GamePhaseEvents.STOP, _ -> {
			for (ServerPlayer player : game.allPlayers()) {
				remove(player, id);
			}
		});
	}

	private void update(ServerPlayer player, Identifier id, int level) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		double amount = level * amountPerLevel;
		AttributeModifier current = instance.getModifier(id);
		if (amount == 0.0) {
			if (current != null) {
				instance.removeModifier(id);
			}
		} else if (current == null || current.amount() != amount) {
			instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
		}
	}

	private void remove(ServerPlayer player, Identifier id) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance != null) {
			instance.removeModifier(id);
		}
	}
}
