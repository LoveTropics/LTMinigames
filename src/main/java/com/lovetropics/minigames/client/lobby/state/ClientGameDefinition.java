package com.lovetropics.minigames.client.lobby.state;

import com.lovetropics.minigames.common.core.game.IGameDefinition;
import com.lovetropics.minigames.common.core.game.config.GameConfigs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

public record ClientGameDefinition(
		Identifier id,
		Component name,
		@Nullable Component subtitle,
		@Nullable Identifier icon,
		int maximumParticipants
) {
	public static final StreamCodec<RegistryFriendlyByteBuf, ClientGameDefinition> STREAM_CODEC = StreamCodec.of((output, definition) -> definition.encode(output), ClientGameDefinition::decode);

	public static List<ClientGameDefinition> collectInstalled() {
		return GameConfigs.REGISTRY.stream()
				.filter(config -> !config.hideFromList())
				.map(ClientGameDefinition::from)
				.collect(Collectors.toList());
	}

	public static ClientGameDefinition from(IGameDefinition definition) {
		return new ClientGameDefinition(
				definition.id(),
				definition.name(),
				definition.subtitle(),
				definition.icon(),
				definition.getMaximumParticipantCount()
		);
	}

	public static ClientGameDefinition decode(RegistryFriendlyByteBuf buffer) {
		Identifier id = buffer.readIdentifier();
		Component name = ComponentSerialization.STREAM_CODEC.decode(buffer);
		Component subtitle = buffer.readBoolean() ? ComponentSerialization.STREAM_CODEC.decode(buffer) : null;
		Identifier icon = buffer.readBoolean() ? buffer.readIdentifier() : null;
		int maximumParticipants = buffer.readVarInt();
		return new ClientGameDefinition(id, name, subtitle, icon, maximumParticipants);
	}

	public void encode(RegistryFriendlyByteBuf buffer) {
		buffer.writeIdentifier(id);
		ComponentSerialization.STREAM_CODEC.encode(buffer, name);
		buffer.writeBoolean(subtitle != null);
		if (subtitle != null) {
			ComponentSerialization.STREAM_CODEC.encode(buffer, subtitle);
		}
		buffer.writeBoolean(icon != null);
		if (icon != null) {
			buffer.writeIdentifier(icon);
		}
		buffer.writeVarInt(maximumParticipants);
	}
}
