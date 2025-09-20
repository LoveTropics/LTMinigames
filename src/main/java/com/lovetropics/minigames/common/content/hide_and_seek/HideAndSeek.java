package com.lovetropics.minigames.common.content.hide_and_seek;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.content.survive_the_tide.SurviveTheTide;
import com.lovetropics.minigames.common.util.registry.GameBehaviorEntry;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

public final class HideAndSeek {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final ItemEntry<Item> NET = REGISTRATE.item("net", Item::new)
			.properties(p -> p.component(DataComponents.LORE, SurviveTheTide.simpleLore(Component.literal("Attack hiding players with the net to capture them!").withStyle(ChatFormatting.GOLD))))
			.register();

	public static final GameBehaviorEntry<HideAndSeekBehavior> HIDE_AND_SEEK = REGISTRATE.object("hide_and_seek")
			.behavior(HideAndSeekBehavior.CODEC)
			.register();

	public static void init() {
	}
}
