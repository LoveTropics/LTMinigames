package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public record AddAttributeModifierAction(Holder<Attribute> attribute, AttributeModifier modifier) implements IGameBehavior {
	public static final MapCodec<AddAttributeModifierAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Attribute.CODEC.fieldOf("attribute").forGetter(AddAttributeModifierAction::attribute),
			AttributeModifier.CODEC.fieldOf("modifier").forGetter(AddAttributeModifierAction::modifier)
	).apply(i, AddAttributeModifierAction::new));

	@Override
	public void register(final IGamePhase game, final EventRegistrar events) {
		events.listen(GameActionEvents.APPLY_TO_LIVING_ENTITY, (context, livingEntity) -> {
			final AttributeInstance attribute = livingEntity.getAttribute(this.attribute);
			if (attribute != null) {
				if (!attribute.hasModifier(modifier.id())) {
					attribute.addTransientModifier(modifier);
				}
				return true;
			}
			return false;
		});
	}
}
