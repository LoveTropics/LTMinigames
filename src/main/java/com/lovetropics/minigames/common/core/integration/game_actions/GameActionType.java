package com.lovetropics.minigames.common.core.integration.game_actions;

import com.lovetropics.minigames.common.config.ConfigLT;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.SharedConstants;
import net.minecraft.util.StringRepresentable;

import java.util.function.Supplier;

public enum GameActionType implements StringRepresentable {
	DONATION("donation", "payment_time", DonationGameAction.CODEC, ConfigLT.GENERAL.donationDelay, false),
	DONATION_PACKAGE("donation_package", "trigger_time", DonationPackageGameAction.CODEC, ConfigLT.GENERAL.donationPackageDelay, true),
	CHAT_EVENT("chat_event", "trigger_time", ChatEventGameAction.CODEC, ConfigLT.GENERAL.chatEventDelay, true),
	;

	public static final Codec<GameActionType> CODEC = StringRepresentable.fromEnum(GameActionType::values);

	private final String id;
	private final String timeFieldName;
	private final Codec<GameActionRequest> codec;
	private final Supplier<Integer> pollingIntervalSeconds;
	private final boolean sendsAcknowledgement;

	@SuppressWarnings("unchecked")
	GameActionType(final String id, String timeFieldName, final MapCodec<? extends GameAction> codec, final Supplier<Integer> pollingIntervalTicks, final boolean sendsAcknowledgement) {
		this.id = id;
		this.timeFieldName = timeFieldName;
		this.codec = GameActionRequest.codec(this, (MapCodec<GameAction>) codec);
		pollingIntervalSeconds = pollingIntervalTicks;
		this.sendsAcknowledgement = sendsAcknowledgement;
	}

	public Codec<GameActionRequest> codec() {
		return codec;
	}

	public String getId() {
		return id;
	}

	public String getTimeFieldName() {
		return timeFieldName;
	}

	public int getPollingIntervalSeconds() {
		return pollingIntervalSeconds.get();
	}

	public int getPollingIntervalTicks() {
		return getPollingIntervalSeconds() * SharedConstants.TICKS_PER_SECOND;
	}

	public boolean sendsAcknowledgement() {
		return sendsAcknowledgement;
	}

	@Override
	public String getSerializedName() {
		return id;
	}
}
