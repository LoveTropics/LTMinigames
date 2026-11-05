package org.lovetropics.games.common.core.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.entity.EntitySubPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/// Matches entities wearing at least `pieces` trimmed armor pieces, no two of which share a trim pattern or a trim material
public record DistinctArmorTrimsPredicate(int pieces) implements EntitySubPredicate {
	public static final Codec<DistinctArmorTrimsPredicate> CODEC = RecordCodecBuilder.create(i -> i.group(
			ExtraCodecs.POSITIVE_INT.optionalFieldOf("pieces", 4).forGetter(DistinctArmorTrimsPredicate::pieces)
	).apply(i, DistinctArmorTrimsPredicate::new));

	@Override
	public boolean matches(Entity entity, ServerLevel level, @Nullable Vec3 position) {
		if (!(entity instanceof LivingEntity living)) {
			return false;
		}
		Set<Holder<TrimPattern>> patterns = new HashSet<>();
		Set<Holder<TrimMaterial>> materials = new HashSet<>();
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
				continue;
			}
			ArmorTrim trim = living.getItemBySlot(slot).get(DataComponents.TRIM);
			if (trim != null && !patterns.contains(trim.pattern()) && !materials.contains(trim.material())) {
				patterns.add(trim.pattern());
				materials.add(trim.material());
			}
		}
		return patterns.size() >= pieces;
	}
}
