package org.lovetropics.dimensions;

import net.minecraft.resources.Identifier;
import net.neoforged.fml.common.Mod;
import org.jetbrains.annotations.ApiStatus;

@Mod(LTDimensionsMod.ID)
@ApiStatus.Internal
public class LTDimensionsMod {
	public static final String ID = "ltdimensions";

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(ID, path);
	}
}
