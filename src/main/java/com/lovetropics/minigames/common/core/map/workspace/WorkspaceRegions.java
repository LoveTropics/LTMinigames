package com.lovetropics.minigames.common.core.map.workspace;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.map.MapRegions;
import com.lovetropics.minigames.common.core.network.workspace.AddWorkspaceRegionMessage;
import com.lovetropics.minigames.common.core.network.workspace.SetWorkspaceMessage;
import com.lovetropics.minigames.common.core.network.workspace.UpdateWorkspaceRegionMessage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

public final class WorkspaceRegions implements Iterable<WorkspaceRegions.Entry> {
	private final ResourceKey<Level> dimension;
	private final Int2ObjectMap<Entry> entries = new Int2ObjectOpenHashMap<>();
	private int nextId;
	private boolean hidden;

	public WorkspaceRegions(ResourceKey<Level> dimension, boolean defaultHidden) {
		this.dimension = dimension;
		hidden = defaultHidden;
	}

	private int nextId() {
		return nextId++;
	}

	public void showHide(ServerPlayer player) {
		hidden = !hidden;
		sendPlayerMessage(createSetWorkspaceMessage(), player);
	}

	public SetWorkspaceMessage createSetWorkspaceMessage() {
		if (hidden) {
			return SetWorkspaceMessage.hidden();
		}
		return new SetWorkspaceMessage(this);
	}

	private <T extends CustomPacketPayload> void sendPlayerMessage(T message, ServerPlayer player) {
		PacketDistributor.sendToPlayer(player, message);
	}

	private <T extends CustomPacketPayload> void sendMessage(@Nullable ServerLevel level, T message) {
		if (level != null) {
			PacketDistributor.sendToPlayersInDimension(level, message);
		}
	}

	public void add(@Nullable ServerLevel level, String key, BlockBox region) {
		add(level, nextId(), key, region);
	}

	public void add(@Nullable ServerLevel level, int id, String key, BlockBox region) {
		add(level, new Entry(id, key, region));
	}

	public boolean rename(@Nullable ServerLevel level, String oldKey, String newKey, BlockPos atPosition) {
		ObjectIterator<Int2ObjectMap.Entry<Entry>> iterator = entries.int2ObjectEntrySet().iterator();
		while (iterator.hasNext()) {
			Int2ObjectMap.Entry<Entry> entry = iterator.next();
			String key = entry.getValue().key;
			BlockBox region = entry.getValue().region;
			if (!region.contains(atPosition) || !key.equals(oldKey)) {
				continue;
			}
			sendMessage(level, new UpdateWorkspaceRegionMessage(entry.getIntKey(), Optional.empty()));
			iterator.remove();
			add(level, newKey, region);
			return true;
		}
		return false;
	}

	void add(@Nullable ServerLevel level, Entry entry) {
		entries.put(entry.id, entry);
		if (level != null) {
			sendMessage(level, new AddWorkspaceRegionMessage(entry.id, entry.key, entry.region));
		}
	}

	public void set(ServerLevel level, int id, @Nullable BlockBox region) {
		if (region != null) {
			WorkspaceRegions.Entry entry = entries.get(id);
			if (entry != null) {
				entry.region = region;
				sendMessage(level, new UpdateWorkspaceRegionMessage(id, Optional.of(region)));
			}
		} else {
			entries.remove(id);
			sendMessage(level, new UpdateWorkspaceRegionMessage(id, Optional.empty()));
		}
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}

	public void write(FriendlyByteBuf buffer) {
		Multimap<String, Entry> byKey = groupedByKey();
		Set<String> keys = byKey.keySet();

		buffer.writeVarInt(keys.size());

		for (String key : keys) {
			buffer.writeUtf(key, 64);

			Collection<Entry> entries = byKey.get(key);
			buffer.writeVarInt(entries.size());

			for (Entry entry : entries) {
				buffer.writeVarInt(entry.id);
				BlockBox.STREAM_CODEC.encode(buffer, entry.region);
			}
		}
	}

	private Multimap<String, Entry> groupedByKey() {
		Multimap<String, Entry> map = HashMultimap.create();
		for (Entry entry : entries.values()) {
			map.put(entry.key, entry);
		}

		return map;
	}

	@Override
	public Iterator<Entry> iterator() {
		return entries.values().iterator();
	}

	public MapRegions compile() {
		MapRegions regions = new MapRegions();
		for (Entry entry : entries.values()) {
			regions.add(entry.key, entry.region);
		}
		return regions;
	}

	public void importFrom(MapRegions regions) {
		entries.clear();
		for (String key : regions.keySet()) {
			for (BlockBox region : regions.get(key)) {
				add(null, key, region);
			}
		}
	}

	public static class Entry {
		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		        Codec.INT.fieldOf("id").forGetter(e -> e.id),
		        Codec.STRING.fieldOf("key").forGetter(e -> e.key),
				MapRegions.LEGACY_BOX_CODEC.fieldOf("region").forGetter(e -> e.region)
		).apply(instance, Entry::new));

		public final int id;
		public final String key;
		public BlockBox region;

		public Entry(int id, String key, BlockBox region) {
			this.id = id;
			this.key = key;
			this.region = region;
		}
	}
}
