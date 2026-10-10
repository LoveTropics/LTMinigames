package org.lovetropics.maps.editor;

import com.tterrag.registrate.Registrate;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.fml.common.Mod;
import org.lovetropics.games.common.core.game.persistent.PersistentGameInstance;
import org.lovetropics.maps.editor.item.EditRegionItem;
import org.lovetropics.maps.editor.map.SavedRegions;

@Mod(MapsEditorMod.ID)
public class MapsEditorMod {
	public static final String ID = "ltmaps_editor";

	public MapsEditorMod() {
		Registrate registrate = Registrate.create(ID).defaultCreativeTab(CreativeModeTabs.OP_BLOCKS);
		registrate.item("edit_region", EditRegionItem::new).register();

		PersistentGameInstance.setRegionsGetter(level -> SavedRegions.get(level).regions().compile());
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(ID, path);
	}
}
