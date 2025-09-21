package com.lovetropics.minigames.gametests.api;

import com.lovetropics.minigames.common.util.LTGameTestFakePlayer;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.stats.Stat;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
public class LTFakePlayer extends ServerPlayer implements LTGameTestFakePlayer {
	public final List<Packet<?>> receivedPackets = new ArrayList<>();
	private final FakePlayerBuilder builder;

	public LTFakePlayer(ServerLevel level, FakePlayerBuilder builder, Consumer<LTFakePlayer> before, String name) {
		super(level.getServer(), level, new GameProfile(UUID.randomUUID(), name), ClientInformation.createDefault());
		this.builder = builder;

		before.accept(this);
		level().getServer().getPlayerList().placeNewPlayer(new net.minecraft.network.Connection(PacketFlow.SERVERBOUND), this, CommonListenerCookie.createInitial(getGameProfile(), false));
	}

	@Override
	public boolean isSpectator() {
		return builder.gameMode == GameType.SPECTATOR;
	}

	@Override
	public boolean isCreative() {
		return builder.gameMode == GameType.SPECTATOR || builder.gameMode.isCreative();
	}

	void exitWorld() {
		connection.disconnect(Component.literal("Test finished"));
	}

	@Override
	public void displayClientMessage(Component chatComponent, boolean actionBar) {

	}

	@Override
	public void awardStat(Stat stat, int amount) {

	}

	@Override
	public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
		return builder.isInvulnerableTo.test(source);
	}

	@Override
	public boolean canHarmPlayer(Player player) {
		return builder.canBeHarmedBy.test(player);
	}

	@Override
	public void die(DamageSource source) {

	}

	@Override
	public void updateOptions(ClientInformation information) {
	}

	@Override
	public void capturePacket(Packet<?> packet) {
		if (builder.packetPredicate.test(packet)) {
			receivedPackets.add(packet);
		}
	}

	@Override
	public boolean shouldRegenerateNaturally() {
		return builder.shouldRegenerateNaturally;
	}
}
