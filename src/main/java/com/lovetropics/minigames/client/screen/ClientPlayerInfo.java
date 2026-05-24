package com.lovetropics.minigames.client.screen;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;

import javax.annotation.Nullable;
import java.util.UUID;

public final class ClientPlayerInfo {
	@Nullable
	public static GameProfile getPlayerProfile(UUID uuid) {
		PlayerInfo info = get(uuid);
		return info != null ? info.getProfile() : null;
	}

	@Nullable
	public static Component getName(UUID uuid) {
		PlayerInfo info = get(uuid);
		if (info != null) {
			Component displayName = info.getTabListDisplayName();
			return displayName != null ? displayName : Component.literal(info.getProfile().name());
		} else {
			return null;
		}
	}

	public static PlayerSkin getSkin(UUID uuid) {
		PlayerInfo info = get(uuid);
		return info != null ? info.getSkin() : DefaultPlayerSkin.get(uuid);
	}

	@Nullable
	public static PlayerInfo get(UUID uuid) {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		return connection != null ? connection.getPlayerInfo(uuid) : null;
	}
}
