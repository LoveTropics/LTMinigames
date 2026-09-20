package org.lovetropics.games.common.core.chat;

import org.lovetropics.games.common.core.game.util.GameTexts;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.stream.Stream;

public enum ChatChannel implements StringRepresentable {
	GLOBAL("global", GameTexts.Commands.GLOBAL_CHAT_CHANNEL),
	TEAM("team", GameTexts.Commands.TEAM_CHAT_CHANNEL),
	;

	public static final EnumCodec<ChatChannel> CODEC = StringRepresentable.fromEnum(ChatChannel::values);

	private final String key;
	private final Component name;

	ChatChannel(String key, Component name) {
		this.key = key;
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return key;
	}

	public Component getName() {
		return name;
	}

	public static Stream<String> names() {
		return Arrays.stream(values()).map(ChatChannel::getSerializedName);
	}
}
