package com.lovetropics.minigames.common.core.game.state.statistics;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.net.Proxy;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PlayerKey implements StatisticHolder {
	private static final YggdrasilAuthenticationService AUTH_SERVICE = new YggdrasilAuthenticationService(Proxy.NO_PROXY, YggdrasilEnvironment.PROD.getEnvironment());
	private static final MinecraftSessionService SESSION_SERVICE = AUTH_SERVICE.createMinecraftSessionService();

	public static final Codec<PlayerKey> FULL_CODEC = RecordCodecBuilder.create(i -> i.group(
			UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(PlayerKey::id),
			Codec.STRING.fieldOf("name").forGetter(PlayerKey::name),
			SkinData.CODEC.optionalFieldOf("skin").forGetter(PlayerKey::skinData)
	).apply(i, (id, name, skinData) -> new PlayerKey(new GameProfile(id, name))));

	// TODO: We should probably update this format :(
	public static final Codec<PlayerKey> UUID_CODEC = UUIDUtil.STRING_CODEC.xmap(
			uuid -> new PlayerKey(new GameProfile(uuid, "Unknown")),
			PlayerKey::id
	);

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
		return profile.id();
	}

	public String name() {
		return profile.name();
	}

	@Override
	public String toString() {
		return profile.name();
	}

	private Optional<SkinData> skinData() {
		MinecraftProfileTexture skinTexture = SESSION_SERVICE.getTextures(profile).skin();
		if (skinTexture != null) {
			return Optional.of(new SkinData(
					skinTexture.getUrl(),
					Objects.requireNonNullElse(skinTexture.getMetadata("model"), "default")
			));
		}
		return Optional.empty();
	}

	public record SkinData(
			String url,
			String model
	) {
		public static final Codec<SkinData> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("url").forGetter(SkinData::url),
				Codec.STRING.fieldOf("model").forGetter(SkinData::model)
		).apply(i, SkinData::new));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj instanceof PlayerKey key) {
			return profile.id().equals(key.profile.id());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return profile.id().hashCode();
	}

	public boolean matches(Entity entity) {
		return entity instanceof ServerPlayer && entity.getUUID().equals(profile.id());
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
