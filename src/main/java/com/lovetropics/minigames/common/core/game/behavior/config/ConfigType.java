package com.lovetropics.minigames.common.core.game.behavior.config;

import java.util.Collection;

public enum ConfigType {

	NONE(Void.class, true),
	STRING(String.class, false),
	NUMBER(Number.class, false),
	BOOLEAN(Boolean.class, false),
	ENUM(Enum.class, false) {
		@Override
		public Object defaultInstance() {
			return requiredType.getEnumConstants()[0];
		}
	},
	LIST(Collection.class, true) {
		@Override
		public Object defaultInstance() {
			return new ConfigData.ListConfigData(NONE);
		}
	},
	COMPOSITE(ConfigData.class, true) {
		@Override
		public Object defaultInstance() {
			return new ConfigData.CompositeConfigData();
		}
	},
	;

	protected final Class<?> requiredType;
	protected final boolean isComplex;

	ConfigType(Class<?> requiredType, boolean isComplex) {
		this.requiredType = requiredType;
		this.isComplex = isComplex;
	}

	public boolean isValidValue(Object val) {
		return requiredType.isInstance(val);
	}

	public boolean isComplex() {
		return isComplex;
	}

	public Object defaultInstance() {
		try {
			return requiredType.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new IllegalArgumentException("Cannot create a default instance for ConfigType " + name(), e);
		}
	}
}
