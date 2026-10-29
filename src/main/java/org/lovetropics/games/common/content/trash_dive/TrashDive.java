package org.lovetropics.games.common.content.trash_dive;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

@EventBusSubscriber(modid = LoveTropics.ID)
public final class TrashDive {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static void init() {
		REGISTRATE.registerBehavior("place_trash", PlaceTrashBehavior.CODEC);
		REGISTRATE.registerBehavior("trash_collection", TrashCollectionBehavior.CODEC);
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
		ScanAreaCommand.register(dispatcher);
	}
}
