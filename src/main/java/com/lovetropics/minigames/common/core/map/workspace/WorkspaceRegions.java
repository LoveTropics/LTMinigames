package com.lovetropics.minigames.common.core.map.workspace;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.map.MapRegions;
import com.lovetropics.minigames.common.core.network.LoveTropicsNetwork;
import com.lovetropics.minigames.common.core.network.workspace.AddWorkspaceRegionMessage;
import com.lovetropics.minigames.common.core.network.workspace.SetWorkspaceMessage;
import com.lovetropics.minigames.common.core.network.workspace.UpdateWorkspaceRegionMessage;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public final class WorkspaceRegions implements Iterable<WorkspaceRegions.Entry> {
	private final ResourceKey<Level> dimension;
	private final Int2ObjectMap<Entry> entries = new Int2ObjectOpenHashMap<>();
	private int nextId;
	private boolean hidden;

	public WorkspaceRegions(ResourceKey<Level> dimension) {
		this.dimension = dimension;
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

	private <T> void sendPlayerMessage(T message, ServerPlayer player) {
		LoveTropicsNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
	}

	private <T> void sendMessage(T message) {
		LoveTropicsNetwork.CHANNEL.send(PacketDistributor.DIMENSION.with(() -> dimension), message);
	}

	public void add(String key, BlockBox region) {
		add(nextId(), key, region);
	}

	public void add(int id, String key, BlockBox region) {
		add(new Entry(id, key, region));
	}

	void add(Entry entry) {
		entries.put(entry.id, entry);
		sendMessage(new AddWorkspaceRegionMessage(entry.id, entry.key, entry.region));
	}

	public void set(int id, @Nullable BlockBox region) {
		if (region != null) {
			WorkspaceRegions.Entry entry = entries.get(id);
			if (entry != null) {
				entry.region = region;
				sendMessage(new UpdateWorkspaceRegionMessage(id, region));
			}
		} else {
			entries.remove(id);
			sendMessage(new UpdateWorkspaceRegionMessage(id, null));
		}
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}

	public CompoundTag write(CompoundTag root) {
		Multimap<String, Entry> byKey = groupedByKey();

		for (String key : byKey.keySet()) {
			Collection<Entry> entries = byKey.get(key);

			ListTag regionsList = new ListTag();
			for (Entry entry : entries) {
				regionsList.add(entry.region.write(new CompoundTag()));
			}

			root.put(key, regionsList);
		}

		return root;
	}

	public void read(CompoundTag root) {
		this.entries.clear();

		for (String key : root.getAllKeys()) {
			ListTag regionsList = root.getList(key, Tag.TAG_COMPOUND);
			for (int i = 0; i < regionsList.size(); i++) {
				BlockBox region = BlockBox.read(regionsList.getCompound(i));
				this.add(key, region);
			}
		}
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
				entry.region.write(buffer);
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
				add(key, region);
			}
		}
	}

	public static class Entry {
		public final int id;
		public final String key;
		public BlockBox region;

		Entry(int id, String key, BlockBox region) {
			this.id = id;
			this.key = key;
			this.region = region;
		}
	}
}
