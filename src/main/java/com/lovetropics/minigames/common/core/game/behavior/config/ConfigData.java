package com.lovetropics.minigames.common.core.game.behavior.config;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ConfigData {

	private final DisplayHint display = DisplayHint.NONE;

	ConfigData() {
	}

	public final DisplayHint display() {
		return display;
	}

	public abstract Object value();

	public final void setValue(Object value) {
		if (type().isValidValue(value)) {
			setValueInternal(value);
		} else {
			throw new IllegalArgumentException("Value is not valid for config type " + type().name() + ": " + value);
		}
	}

	protected abstract void setValueInternal(Object value);

	public abstract ConfigType type();

	public static class SimpleConfigData extends ConfigData {

		private final ConfigType type;
		private Object value;

		public SimpleConfigData(ConfigType type) {
			this(type, null);
		}

		public SimpleConfigData(ConfigType type, Object value) {
			if (type == ConfigType.COMPOSITE || type == ConfigType.LIST) {
				throw new IllegalArgumentException("Simple config data cannot be of type COMPOSITE or LIST");
			}
			this.type = type;
			this.value = value;
		}

		@Override
		public Object value() {
			return value;
		}

		@Override
		protected void setValueInternal(Object value) {
			this.value = value;
		}

		@Override
		public ConfigType type() {
			return type;
		}

		@Override
		public String toString() {
			return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
					.append("type", type())
					.append("value", value)
					.build();
		}
	}

	public static class ListConfigData extends ConfigData implements Iterable<Object> {

		public static final ListConfigData EMPTY = new ListConfigData(ConfigType.NONE);

		private ConfigType type;
		@Nullable
		private Object defaultValue;
		private final List<Object> values = new ArrayList<>();

		public ListConfigData(ConfigType type) {
			this.type = type;
		}

		public ListConfigData setDefaultValue(Object defaultValue) {
			this.defaultValue = defaultValue;
			return this;
		}

		public void addDefault() {
			if (defaultValue != null) {
				add(defaultValue);
			} else {
				add(type.defaultInstance());
			}
		}

		public ListConfigData setComponentType(ConfigType type) {
			if (type == this.type) {
				return this;
			}
			if (this.type != ConfigType.NONE) {
				throw new IllegalStateException("List component type already set");
			}
			this.type = type;
			return this;
		}

		public void add(Object value) {
			if (componentType() == ConfigType.NONE) {
				for (ConfigType type : ConfigType.values()) {
					if (type.isValidValue(value)) {
						this.type = type;
						break;
					}
				}
			}
			if (componentType().isValidValue(value)) {
				values.add(value);
			} else {
				throw new IllegalArgumentException("Invalid list value for type " + componentType() + ": " + value.getClass().getSimpleName() + " " + value);
			}
		}

		public void addAll(Collection<?> value) {
			value.forEach(this::add);
		}

		@Override
		public List<Object> value() {
			return values;
		}

		@Override
		protected void setValueInternal(Object value) {
			if (value instanceof Collection<?> coll) {
				if (!coll.isEmpty()) {
					if (coll.stream().allMatch(componentType()::isValidValue)) {
						values.clear();
						values.addAll(coll);
					} else {
						throw new IllegalArgumentException("List contains invalid values for component type " + componentType() + ": " + value);
					}
				} else {
					values.clear();
				}
			} else if (value instanceof Object[] arr) {
				if (arr.length > 0) {
					if (Arrays.stream(arr).allMatch(componentType()::isValidValue)) {
						values.clear();
						values.addAll(Arrays.asList(arr));
					} else {
						throw new IllegalArgumentException("Array contains invalid values for component type " + componentType() + ": " + Arrays.toString(arr));
					}
				} else {
					values.clear();
				}
			} else {
				throw new IllegalArgumentException("Value is not valid for list type " + componentType() + ": " + value);
			}
		}

		@Override
		public ConfigType type() {
			return ConfigType.LIST;
		}

		public ConfigType componentType() {
			return type;
		}

		@Override
		public String toString() {
			return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
					.append("componentType", componentType())
					.append("values", values)
					.build();
		}

		@Override
		public Iterator<Object> iterator() {
			return values.iterator();
		}
	}

	public static class CompositeConfigData extends ConfigData {
		private final Map<String, ConfigData> values = new HashMap<>();

		public final void addChild(String name, ConfigData config) {
			values.put(name, config);
		}

		@Override
		public Map<String, ConfigData> value() {
			return Collections.unmodifiableMap(values);
		}

		public ConfigData value(String name) {
			return values.get(name);
		}

		public Set<String> keys() {
			return values.keySet();
		}

		@Nullable
		public ConfigData put(String name, ConfigData value) {
			return values.put(name, value);
		}

		@Override
		protected void setValueInternal(Object value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ConfigType type() {
			return ConfigType.COMPOSITE;
		}

		@Override
		public String toString() {
			return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
					.append("values", values)
					.build();
		}
	}
}
