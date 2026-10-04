package org.lovetropics.maps.editor;

import com.tterrag.registrate.Registrate;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import org.lovetropics.games.common.core.game.persistent.PersistentGameInstance;
import org.lovetropics.maps.editor.item.EditRegionItem;
import org.lovetropics.maps.editor.map.SavedRegions;
import org.lovetropics.maps.editor.workspace.MapWorkspaceManager;

@Mod(MapsEditorMod.ID)
public class MapsEditorMod {
	public static final String ID = "ltmaps_editor";

	public MapsEditorMod() {
		NeoForge.EVENT_BUS.addListener(this::onAttemptSpawn);

		Registrate registrate = Registrate.create(ID);
		registrate.item("edit_region", EditRegionItem::new).register();

		PersistentGameInstance.setRegionsGetter(level -> SavedRegions.get(level).regions().compile());
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(ID, path);
	}

	private void onAttemptSpawn(MobSpawnEvent.PositionCheck event) {
		if (event.getSpawnType() == EntitySpawnReason.SPAWNER) {
			MapWorkspaceManager workspace = MapWorkspaceManager.get(event.getLevel().getServer());
			if (workspace.getWorkspace(event.getLevel().getLevel().dimension()) != null) {
				event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
			}
		}
	}
}
