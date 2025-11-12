package com.lovetropics.minigames.common.core.game.player;

import com.lovetropics.lib.codec.MoreCodecs;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

public enum PlayerRole {
	PARTICIPANT("participant"),
	SPECTATOR("spectator");

	public static final PlayerRole[] ROLES = values();
	private static final Map<String, PlayerRole> BY_KEY = new Object2ObjectOpenHashMap<>(ROLES.length);

	public static final Codec<PlayerRole> CODEC = MoreCodecs.stringVariants(ROLES, PlayerRole::getKey);

	private final String key;

	PlayerRole(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	public static Stream<PlayerRole> stream() {
		return Arrays.stream(ROLES);
	}

	@Nullable
	public static PlayerRole byKey(String key) {
		return BY_KEY.get(key);
	}

	static {
		for (PlayerRole role : ROLES) {
			BY_KEY.put(role.key, role);
		}
	}
}
