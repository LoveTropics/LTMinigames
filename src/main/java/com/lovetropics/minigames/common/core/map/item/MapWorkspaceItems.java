package com.lovetropics.minigames.common.core.map.item;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;

public class MapWorkspaceItems {
    
    private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();
 
    public static final ItemEntry<EditRegionItem> EDIT_REGION = REGISTRATE.item("edit_region", EditRegionItem::new)
            .register();
    
    public static void init() {}
}
