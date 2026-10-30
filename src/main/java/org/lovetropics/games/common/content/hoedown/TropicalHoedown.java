package org.lovetropics.games.common.content.hoedown;

import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.common.util.registry.LoveTropicsRegistrate;

/// Mysticat's Tropical Hoedown of 2026
public class TropicalHoedown {
	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static void init() {
		REGISTRATE.registerBehavior("derby_fireworks", DerbyFireworksBehavior.CODEC);
	}
}
