package com.lovetropics.minigames.common.core.game.state.statistics;

import com.google.gson.JsonObject;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import javax.annotation.Nullable;

public record CauseOfDeath(String type, String typeName, @Nullable String source, @Nullable String sourceName) {
	public static CauseOfDeath from(DamageSource damage) {
		String type = damage.getMsgId();
		String typeName = getDamageTypeName(damage);

		String source = null;
		String sourceName = null;

		Entity entitySource = damage.getEntity();
		if (entitySource != null) {
			EntityType<?> sourceType = entitySource.getType();
			source = EntityType.getKey(sourceType).toString();
			sourceName = sourceType.getDescription().getString();
		}

		return new CauseOfDeath(type, typeName, source, sourceName);
	}

	public JsonObject serialize() {
		JsonObject root = new JsonObject();
		root.addProperty("type", type);
		root.addProperty("type_name", typeName);

		if (source != null) {
			root.addProperty("source", source);
			root.addProperty("source_name", sourceName);
		}

		return root;
	}

	private static String getDamageTypeName(DamageSource damage) {
		if (damage.is(DamageTypes.IN_FIRE)) {
			return "went up in flames";
		} else if (damage.is(DamageTypes.LIGHTNING_BOLT)) {
			return "was struck by lightning";
		} else if (damage.is(DamageTypes.ON_FIRE)) {
			return "burned to death";
		} else if (damage.is(DamageTypes.LAVA)) {
			return "tried to swim in lava";
		} else if (damage.is(DamageTypes.HOT_FLOOR)) {
			return "discovered the floor was lava";
		} else if (damage.is(DamageTypes.IN_WALL)) {
			return "suffocated in a wall";
		} else if (damage.is(DamageTypes.CRAMMING)) {
			return "squished too much";
		} else if (damage.is(DamageTypes.DROWN)) {
			return "drowned";
		} else if (damage.is(DamageTypes.STARVE)) {
			return "starved to death";
		} else if (damage.is(DamageTypes.CACTUS)) {
			return "pricked to death";
		} else if (damage.is(DamageTypes.FALL)) {
			return "hit the ground too hard";
		} else if (damage.is(DamageTypes.FLY_INTO_WALL)) {
			return "experienced kinetic energy";
		} else if (damage.is(DamageTypes.FELL_OUT_OF_WORLD)) {
			return "fell out of the world";
		} else if (damage.is(DamageTypes.MAGIC)) {
			return "killed by magic";
		} else if (damage.is(DamageTypes.WITHER)) {
			return "withered to death";
		} else if (damage.is(DamageTypes.FALLING_ANVIL)) {
			return "squashed by a falling anvil";
		} else if (damage.is(DamageTypes.FALLING_BLOCK)) {
			return "squashed by a falling block";
		} else if (damage.is(DamageTypes.DRAGON_BREATH)) {
			return "roasted in dragon breath";
		} else if (damage.is(DamageTypes.SWEET_BERRY_BUSH)) {
			return "poked to death by a sweet berry bush";
		} else if (damage.getEntity() != null) {
			return "was slain";
		}

		return "died";
	}
}
