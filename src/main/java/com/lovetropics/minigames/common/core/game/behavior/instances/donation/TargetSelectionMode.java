package com.lovetropics.minigames.common.core.game.behavior.instances.donation;

import com.lovetropics.lib.codec.MoreCodecs;
import com.mojang.serialization.Codec;

public enum TargetSelectionMode {
	SPECIFIC("specific"),
	RANDOM("random"),
	ALL("all"),
	;

	public static final Codec<TargetSelectionMode> CODEC = MoreCodecs.stringVariants(TargetSelectionMode.values(), s -> s.type);

	public final String type;

	TargetSelectionMode(String type) {
		this.type = type;
	}
}
