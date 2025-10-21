package com.lovetropics.minigames.common.core.diguise.ability;

public interface DisguiseAbilities {
	final class Composite implements DisguiseAbilities {
		private final DisguiseAbilities[] abilities;

		public Composite(DisguiseAbilities[] abilities) {
			this.abilities = abilities;
		}
	}
}
