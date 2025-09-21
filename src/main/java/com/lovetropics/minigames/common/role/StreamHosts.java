package com.lovetropics.minigames.common.role;

import com.lovetropics.lib.permission.PermissionsApi;
import com.lovetropics.lib.permission.role.RoleOverrideType;
import com.lovetropics.lib.permission.role.RoleReader;
import com.mojang.serialization.Codec;
import net.minecraft.world.entity.player.Player;

public class StreamHosts {
	public static final RoleOverrideType<Boolean> ROLE_OVERRIDE = RoleOverrideType.register("host", Codec.BOOL);

	public static void init() {
	}

	public static boolean isHost(Player player) {
		RoleReader roles = PermissionsApi.lookup().byEntity(player);
		return roles.overrides().test(ROLE_OVERRIDE);
	}
}
