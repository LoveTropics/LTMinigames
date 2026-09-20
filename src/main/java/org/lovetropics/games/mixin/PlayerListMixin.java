package org.lovetropics.games.mixin;

import org.lovetropics.games.common.core.game.PlayerIsolation;
import org.lovetropics.games.common.core.game.PlayerListAccess;
import org.lovetropics.games.common.core.game.impl.GameLobbyManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin implements PlayerListAccess {
	@Shadow
	@Final
	private List<ServerPlayer> players;
	@Shadow
	@Final
	private Map<UUID, ServerPlayer> playersByUUID;

	@Shadow
	protected abstract void save(ServerPlayer player);

	@Inject(method = "save", at = @At("HEAD"), cancellable = true)
	private void save(ServerPlayer player, CallbackInfo ci) {
		if (PlayerIsolation.INSTANCE.isIsolated(player)) {
			ci.cancel();
		}
	}

	@Override
	public void ltminigames$save(ServerPlayer player) {
		save(player);
	}

	@Override
	public void ltminigames$remove(ServerPlayer player) {
		players.remove(player);
	}

	@Override
	public void ltminigames$add(ServerPlayer player) {
		players.add(player);
		playersByUUID.put(player.getUUID(), player);
	}

	@Override
	public void ltminigames$firePlayerLoading(ServerPlayer player) {
		EventHooks.firePlayerLoadingEvent(player, (PlayerList) (Object) (this), player.getStringUUID());
	}

	// We need to run the rest of the logic with the new player instance
	// FIXME: It would be nice to not need to produce new player instances in the logout process - but microgames need it right now to pull players all the way out
	@ModifyVariable(method = "remove", at = @At(value = "HEAD"), argsOnly = true)
	private ServerPlayer onPlayerLogOut(ServerPlayer player) {
		return GameLobbyManager.onPlayerLoggedOut(player);
	}
}
