package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.TypedEntityData;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.peekaboo.api.Disguise;
import org.lovetropics.peekaboo.api.EntityDisguiseHolder;

public record ClearDisguiseAction(
		Disguise disguise,
		boolean ignoreEntityData
) implements IGameBehavior {
	public static final MapCodec<ClearDisguiseAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Disguise.MAP_CODEC.forGetter(ClearDisguiseAction::disguise),
			Codec.BOOL.optionalFieldOf("ignore_entity_data", false).forGetter(ClearDisguiseAction::ignoreEntityData)
	).apply(i, ClearDisguiseAction::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.applyToEntities(game, (context, level, entity) -> {
			// Nothing specified to clear - just clear everything
			if (disguise.isEmpty()) {
				EntityDisguiseHolder.set(entity, Disguise.NONE);
			} else {
				EntityDisguiseHolder.update(entity, this::clearDisguiseFrom);
			}
			return true;
		});
	}

	private Disguise clearDisguiseFrom(Disguise currentDisguise) {
		// Future reader: yes, this is stupid
		if (ignoreEntityData) {
			Disguise currentWithoutData = removeEntityData(currentDisguise);
			Disguise clearedWithoutData = currentWithoutData.clear(disguise);
			if (!clearedWithoutData.equals(currentWithoutData)) {
				return clearedWithoutData;
			}
		}
		return currentDisguise.clear(disguise);
	}

	private static Disguise removeEntityData(Disguise disguise) {
		return disguise.withEntity(disguise.entity().map(data -> TypedEntityData.of(data.type(), new CompoundTag())));
	}
}
