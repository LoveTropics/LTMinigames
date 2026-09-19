package com.lovetropics.minigames.common.content.trash_dive;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.util.registry.GameBehaviorEntry;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = LoveTropics.ID)
public final class TrashDive {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final GameBehaviorEntry<PlaceTrashBehavior> PLACE_TRASH = REGISTRATE.object("place_trash")
			.behavior(PlaceTrashBehavior.CODEC)
			.register();
	public static final GameBehaviorEntry<TrashCollectionBehavior> TRASH_COLLECTION = REGISTRATE.object("trash_collection")
			.behavior(TrashCollectionBehavior.CODEC)
			.register();

	public static void init() {
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
		ScanAreaCommand.register(dispatcher);
	}
}
