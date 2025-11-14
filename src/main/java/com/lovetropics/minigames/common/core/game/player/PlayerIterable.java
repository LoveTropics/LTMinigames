package com.lovetropics.minigames.common.core.game.player;

import com.google.common.base.Predicate;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import net.minecraft.Util;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface PlayerIterable extends PlayerOps, Iterable<ServerPlayer> {
	default PlayerIterable filter(Predicate<? super ServerPlayer> predicate) {
		return () -> Iterators.filter(iterator(), predicate);
	}

	default PlayerIterable excluding(ServerPlayer player) {
		return filter(target -> target != player);
	}

	@Override
	Iterator<ServerPlayer> iterator();

	default Stream<ServerPlayer> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	@Override
	default void sendMessage(Component message, boolean actionBar) {
		for (ServerPlayer player : this) {
			player.displayClientMessage(message, actionBar);
		}
	}

	@Override
	default void addPotionEffect(MobEffectInstance effect) {
		for (ServerPlayer player : this) {
			player.addEffect(new MobEffectInstance(effect));
		}
	}

	@Override
	default void playSound(SoundEvent sound, SoundSource category, float volume, float pitch) {
		for (ServerPlayer player : this) {
			player.playNotifySound(sound, category, volume, pitch);
		}
	}

	@Override
	default void sendPacket(Packet<?> packet) {
		for (ServerPlayer player : this) {
			player.connection.send(packet);
		}
	}

	@Override
	default void sendPacket(CustomPacketPayload message) {
		for (ServerPlayer player : this) {
			PacketDistributor.sendToPlayer(player, message);
		}
	}

	default void showTitle(@Nullable final Component title, @Nullable final Component subtitle, final int fadeIn, final int stay, final int fadeOut) {
		sendPacket(new ClientboundClearTitlesPacket(true));
		sendPacket(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
		sendPacket(new ClientboundSetTitleTextPacket(title != null ? title : CommonComponents.space()));
		if (subtitle != null) {
			sendPacket(new ClientboundSetSubtitleTextPacket(subtitle));
		}
	}

	default void showTitle(final Component title, final int fadeIn, final int stay, final int fadeOut) {
		showTitle(title, null, fadeIn, stay, fadeOut);
	}

	static Iterator<ServerPlayer> resolvingIterator(MinecraftServer server, Iterator<UUID> ids) {
		PlayerList playerList = server.getPlayerList();
		return new AbstractIterator<>() {
			@Override
			protected ServerPlayer computeNext() {
				while (true) {
					if (!ids.hasNext()) {
						return endOfData();
					}

					UUID id = ids.next();
					ServerPlayer player = playerList.getPlayer(id);
					if (player != null) {
						return player;
					}
				}
			}
		};
	}

	default List<ServerPlayer> shuffledCopy(RandomSource random) {
		List<ServerPlayer> players = Lists.newArrayList(iterator());
		Util.shuffle(players, random);
		return players;
	}
}
