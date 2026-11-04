package org.lovetropics.games.common.core.game.behavior.instances.action;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lovetropics.games.common.core.game.GameException;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.action.GameActionContextKeys;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.network.ClientboundPlayerFaceDVDPackets;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record AddPlayerDVDFaceAction(
		Optional<UUID> uuid,
		boolean applyDonorName,
		int lengthInTicks,
		int count
) implements IGameBehavior {

	public static final MapCodec<AddPlayerDVDFaceAction> CODEC = RecordCodecBuilder.<AddPlayerDVDFaceAction>mapCodec(i -> i.group(
			UUIDUtil.CODEC.optionalFieldOf("uuid").forGetter(AddPlayerDVDFaceAction::uuid),
			Codec.BOOL.optionalFieldOf("apply_donor_name", false).forGetter(AddPlayerDVDFaceAction::applyDonorName),
			Codec.INT.fieldOf("length_in_ticks").forGetter(AddPlayerDVDFaceAction::lengthInTicks),
			ExtraCodecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(AddPlayerDVDFaceAction::count)
	).apply(i, AddPlayerDVDFaceAction::new)).validate(action -> {
		if (action.uuid.isEmpty() && !action.applyDonorName) {
			return DataResult.error(() -> "Must define uuid or apply_donor_name");
		}
		return DataResult.success(action);
	});

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.applyToPlayers(game, (context, player) -> {
			String packageSender = context.getOptional(GameActionContextKeys.PACKAGE_SENDER);
			if (applyDonorName && packageSender != null) {
				resolveSenderId(game, packageSender).thenAcceptAsync(
						donorId -> PacketDistributor.sendToPlayer(player, new ClientboundPlayerFaceDVDPackets.Add(donorId, lengthInTicks, count)),
						game.scheduler()
				);
				return true;
			}
			PacketDistributor.sendToPlayer(player, new ClientboundPlayerFaceDVDPackets.Add(uuid.orElse(Util.NIL_UUID), lengthInTicks, count));
			return true;
		});
	}

	private static CompletableFuture<UUID> resolveSenderId(IGamePhase game, String playerName) {
		ProfileResolver resolver = game.server().services().profileResolver();
		return CompletableFuture.supplyAsync(() -> resolver.fetchByName(playerName), Util.nonCriticalIoPool())
				.thenApply(maybeProfile -> maybeProfile.map(GameProfile::id).orElse(Util.NIL_UUID));
	}

	@Override
	public MapCodec<? extends IGameBehavior> codec() {
		return CODEC;
	}
}
