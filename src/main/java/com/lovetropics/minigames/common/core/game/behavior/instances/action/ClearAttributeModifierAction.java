package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

public record ClearAttributeModifierAction(Holder<Attribute> attribute, ResourceLocation id) implements IGameBehavior {
	public static final MapCodec<ClearAttributeModifierAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Attribute.CODEC.fieldOf("attribute").forGetter(ClearAttributeModifierAction::attribute),
			ResourceLocation.CODEC.fieldOf("id").forGetter(ClearAttributeModifierAction::id)
	).apply(i, ClearAttributeModifierAction::new));

	@Override
	public void register(final IGamePhase game, final EventRegistrar events) {
		events.listen(GameActionEvents.APPLY_TO_LIVING_ENTITY, (context, livingEntity) -> {
			final AttributeInstance attribute = livingEntity.getAttribute(this.attribute);
			if (attribute != null) {
				attribute.removeModifier(id);
				return true;
			}
			return false;
		});
	}
}
