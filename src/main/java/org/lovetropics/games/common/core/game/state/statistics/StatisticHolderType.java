package org.lovetropics.games.common.core.game.state.statistics;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum StatisticHolderType implements StringRepresentable {
	/// [#TEAM] if [org.lovetropics.games.common.core.game.state.team.TeamState] is set up in the game, else [#PLAYER].
	DEFAULT("default"),
	PLAYER("player"),
	TEAM("team"),
	;

	public static final Codec<StatisticHolderType> CODEC = StringRepresentable.fromEnum(StatisticHolderType::values);

	private final String id;

	StatisticHolderType(String id) {
		this.id = id;
	}

	@Override
	public String getSerializedName() {
		return id;
	}
}
