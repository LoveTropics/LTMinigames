package com.lovetropics.minigames.common.util.world.gamedata;

import com.lovetropics.minigames.LoveTropics;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameDataStorage extends SavedData {
	private static final Identifier ID = LoveTropics.location("gamedata");

	public record NamespacedData(Map<UUID, CompoundTag> playerData) {
		public static final Codec<NamespacedData> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.unboundedMap(UUIDUtil.STRING_CODEC, CompoundTag.CODEC).fieldOf("playerData").forGetter(NamespacedData::playerData)
		).apply(i, NamespacedData::new));

		public NamespacedData(Map<UUID, CompoundTag> playerData) {
			this.playerData = new HashMap<>(playerData);
		}
	}

	private static final Codec<GameDataStorage> INNER_CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.unboundedMap(Identifier.CODEC, NamespacedData.CODEC).fieldOf("playerData").forGetter(GameDataStorage::getPlayerData)
	).apply(i, GameDataStorage::new));

	// TODO: Why the extra wrapping?
	public static final Codec<GameDataStorage> CODEC = INNER_CODEC.fieldOf("namespaces").codec();

	public static final SavedDataType<GameDataStorage> TYPE = new SavedDataType<>(
			ID,
			GameDataStorage::new,
			CODEC
	);

	protected final Map<Identifier, NamespacedData> playerData;

	public Map<Identifier, NamespacedData> getPlayerData() {
		return playerData;
	}

	public GameDataStorage() {
		this(Map.of());
	}

	private GameDataStorage(Map<Identifier, NamespacedData> playerData) {
		this.playerData = new HashMap<>(playerData);
	}

	public static GameDataStorage get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	public CompoundTag get(Identifier storageId, UUID playerId) {
		return getNamespacedData(storageId).playerData().computeIfAbsent(playerId, k -> new CompoundTag());
	}

	private NamespacedData getNamespacedData(Identifier storageId) {
		return playerData.computeIfAbsent(storageId, k -> new NamespacedData(new Object2ObjectOpenHashMap<>()));
	}

	public void set(Identifier storageId, UUID playerId, CompoundTag tag) {
		getNamespacedData(storageId).playerData().put(playerId, tag);
		setDirty();
	}
}
