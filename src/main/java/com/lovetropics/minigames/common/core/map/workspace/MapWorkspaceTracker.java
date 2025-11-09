package com.lovetropics.minigames.common.core.map.workspace;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.network.workspace.SetWorkspaceMessage;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = LoveTropics.ID)
public final class MapWorkspaceTracker {
	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			trySendWorkspace(player, player.level().dimension());
		}
	}

	@SubscribeEvent
	public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		Player player = event.getEntity();
		ResourceKey<Level> dimension = event.getTo();
		trySendWorkspace((ServerPlayer) player, dimension);
	}

	private static void trySendWorkspace(ServerPlayer player, ResourceKey<Level> dimension) {
		MinecraftServer server = player.level().getServer();
		MapWorkspaceManager workspaceManager = MapWorkspaceManager.get(server);

		WorkspaceRegions regions = workspaceManager.getRegions(server, dimension);

		if (regions != null) {
			SetWorkspaceMessage message = regions.createSetWorkspaceMessage();
			PacketDistributor.sendToPlayer(player, message);
		}
	}
}
