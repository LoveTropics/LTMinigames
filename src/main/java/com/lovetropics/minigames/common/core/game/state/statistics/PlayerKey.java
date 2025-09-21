package com.lovetropics.minigames.common.core.game.state.statistics;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.net.Proxy;
import java.util.UUID;

public final class PlayerKey implements StatisticHolder {
	private static final YggdrasilAuthenticationService AUTH_SERVICE = new YggdrasilAuthenticationService(Proxy.NO_PROXY, YggdrasilEnvironment.PROD.getEnvironment());
	private static final MinecraftSessionService SESSION_SERVICE = AUTH_SERVICE.createMinecraftSessionService();

	private final GameProfile profile;

	private PlayerKey(GameProfile profile) {
		this.profile = profile;
	}

	public static PlayerKey from(GameProfile profile) {
		return new PlayerKey(profile);
	}

	public static PlayerKey from(Player player) {
		return new PlayerKey(player.getGameProfile());
	}

	public UUID id() {
		return profile.getId();
	}

	public String name() {
		return profile.getName();
	}

	@Override
	public String toString() {
		return profile.getName();
	}

	public JsonElement serializeProfile() {
		JsonObject root = new JsonObject();
		root.addProperty("id", profile.getId().toString());
		root.addProperty("name", profile.getName());

		MinecraftProfileTexture skinTexture = SESSION_SERVICE.getTextures(profile).skin();
		if (skinTexture != null) {
			JsonObject skinRoot = new JsonObject();
			skinRoot.addProperty("url", skinTexture.getUrl());

			String model = skinTexture.getMetadata("model");
			if (model == null) {
				model = "default";
			}

			skinRoot.addProperty("model", model);

			root.add("skin", skinRoot);
		}

		return root;
	}

	public JsonElement serializeId() {
		return new JsonPrimitive(profile.getId().toString());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj instanceof PlayerKey key) {
			return profile.getId().equals(key.profile.getId());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return profile.getId().hashCode();
	}

	public boolean matches(Entity entity) {
		return entity instanceof ServerPlayer && entity.getUUID().equals(profile.getId());
	}

	@Override
	public Component getName(IGamePhase game) {
		TeamState teams = game.instanceState().getOrNull(TeamState.KEY);
		GameTeamKey team = teams != null ? teams.getTeamForPlayer(id()) : null;
		if (team != null) {
			return Component.literal(name()).withStyle(teams.getTeamOrThrow(team).config().formatting());
		}
		return Component.literal(name());
	}

	@Override
	public StatisticsMap getOwnStatistics(GameStatistics statistics) {
		return statistics.forPlayer(this);
	}
}
