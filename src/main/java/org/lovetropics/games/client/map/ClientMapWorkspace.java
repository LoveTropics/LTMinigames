package org.lovetropics.games.client.map;

import com.lovetropics.lib.BlockBox;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.core.map.workspace.ClientWorkspaceRegions;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

import org.jspecify.annotations.Nullable;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public final class ClientMapWorkspace {
	public static final ClientMapWorkspace INSTANCE = new ClientMapWorkspace();

	private ClientWorkspaceRegions regions = new ClientWorkspaceRegions();

	private ClientMapWorkspace() {
	}

	public void setRegions(ClientWorkspaceRegions regions) {
		this.regions = regions;
	}

	public void addRegion(int id, String key, BlockBox region) {
		regions.add(id, key, region);
	}

	public void updateRegion(int id, @Nullable BlockBox region) {
		regions.set(id, region);
	}

	public ClientWorkspaceRegions getRegions() {
		return regions;
	}

	private void reset() {
		regions = new ClientWorkspaceRegions();
	}

	@SubscribeEvent
	public static void onChangeDimension(LevelEvent.Load event) {
		LevelAccessor level = event.getLevel();
		if (level.isClientSide()) {
			INSTANCE.reset();
		}
	}
}
