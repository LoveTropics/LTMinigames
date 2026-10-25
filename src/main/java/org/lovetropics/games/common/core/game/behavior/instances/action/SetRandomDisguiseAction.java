package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.peekaboo.api.Disguise;
import org.lovetropics.peekaboo.api.EntityDisguiseHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SetRandomDisguiseAction implements IGameBehavior {
	public static final MapCodec<SetRandomDisguiseAction> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE).optionalFieldOf("disallowed", HolderSet.empty()).forGetter(actions -> actions.disallowedEntities),
			Disguise.CODEC.listOf().optionalFieldOf("additional", Collections.emptyList()).forGetter(actions -> actions.additionalDisguises)
	).apply(i, SetRandomDisguiseAction::new));

	private final HolderSet<EntityType<?>> disallowedEntities;
	private final List<Disguise> additionalDisguises;

	private final List<Disguise> allDisguises;

	public SetRandomDisguiseAction(HolderSet<EntityType<?>> disallowedEntities, List<Disguise> additionalDisguises) {
		this.disallowedEntities = disallowedEntities;
		this.additionalDisguises = additionalDisguises;

		this.allDisguises = new ArrayList<>();
		this.allDisguises.addAll(additionalDisguises);

		for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
			if (!disallowedEntities.contains(Holder.direct(entityType))) {
				allDisguises.add(Disguise.of(entityType));
			}
		}
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		events.applyToPlayers(game, (context, player) -> {
			EntityDisguiseHolder disguiseHolder = EntityDisguiseHolder.getOrNull(player);
			if (disguiseHolder == null) {
				return false;
			}

			int i = game.random().nextInt(allDisguises.size());
			Disguise disguise = allDisguises.get(i);
			disguiseHolder.set(disguise);
			return true;
		});
	}


}
