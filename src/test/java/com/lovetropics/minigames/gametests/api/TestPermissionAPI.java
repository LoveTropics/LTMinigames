package com.lovetropics.minigames.gametests.api;

import com.lovetropics.lib.permission.role.Role;
import com.lovetropics.lib.permission.role.RoleLookup;
import com.lovetropics.lib.permission.role.RoleOverrideReader;
import com.lovetropics.lib.permission.role.RoleOverrideType;
import com.lovetropics.lib.permission.role.RoleReader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class TestPermissionAPI implements RoleLookup {
	private final Map<UUID, Roles> roles = new HashMap<>();

	@NotNull
	@Override
	public Roles byEntity(Entity entity) {
		return roles.computeIfAbsent(entity.getUUID(), id -> new Roles());
	}

	@NotNull
	@Override
	public Roles bySource(CommandSourceStack commandSourceStack) {
		return byEntity(Objects.requireNonNull(commandSourceStack.getEntity()));
	}

	public static class Roles implements RoleReader {
		private final List<Role> roles = new ArrayList<>();
		private final RoleOverrideMap overrides = new RoleOverrideMap(new HashMap<>());

		@Override
		public boolean has(Role role) {
			return roles.contains(role);
		}

		@Override
		public RoleOverrideReader overrides() {
			return overrides;
		}

		public void addRole(String id, Map<RoleOverrideType<?>, Object> overrides) {
			roles.add(new SimpleRole(id, new RoleOverrideMap(overrides), roles.isEmpty() ? 0 : roles.getLast().index() + 1));
			rebuild();
		}

		public void removeRole(String id) {
			if (roles.removeIf(r -> r.id().equals(id))) {
				rebuild();
			}
		}

		private void rebuild() {
			overrides.map.clear();
			for (Role role : roles) {
				overrides.map.putAll(((SimpleRole) role).overrides.map);
			}
		}

		@NotNull
		@Override
		public Iterator<Role> iterator() {
			return roles.iterator();
		}
	}

	public record SimpleRole(String id, RoleOverrideMap overrides, int index) implements Role {
		public static SimpleRole empty(String id) {
			return new SimpleRole(id, new RoleOverrideMap(Map.of()), 0);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			return obj instanceof SimpleRole role && index == role.index && role.id.equalsIgnoreCase(id);
		}

		@Override
		public int hashCode() {
			return id.hashCode();
		}

		@Override
		public String toString() {
			return "\"" + id + "\" (" + index + ")";
		}
	}

	public record RoleOverrideMap(Map<RoleOverrideType<?>, Object> map) implements RoleOverrideReader {

		@Nullable
		@Override
		public <T> T getOrNull(RoleOverrideType<T> roleOverrideType) {
			return (T) map.get(roleOverrideType);
		}

		@Override
		public Set<RoleOverrideType<?>> typeSet() {
			return map.keySet();
		}
	}
}
