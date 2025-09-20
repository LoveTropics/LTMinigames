package com.lovetropics.minigames.common.core.game.behavior.instances.tweak;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;

public class HungerResetter {
	private static final CompoundTag FRESH_TAG;

	static {
		TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
		FoodData foodStats = new FoodData();
		foodStats.addAdditionalSaveData(output);
		FRESH_TAG = output.buildResult();
	}

	public static void reset(ServerPlayer player) {
		ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), FRESH_TAG);
		player.getFoodData().readAdditionalSaveData(input);
	}
}
