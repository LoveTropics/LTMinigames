package org.lovetropics.games.mixin;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import org.lovetropics.games.common.core.advancement.LoveTropicsCriteria;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Mixin(Raid.class)
public abstract class RaidMixin {
	@Shadow
	@Final
	private ServerBossEvent raidEvent;

	@Shadow
	@Final
	private Set<UUID> heroesOfTheVillage;

	@Shadow
	private int groupsSpawned;

	@Shadow
	public abstract int getTotalRaidersAlive();

	@Inject(method = "removeFromRaid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/raid/Raid;setDirty(Lnet/minecraft/server/level/ServerLevel;)V", shift = At.Shift.AFTER))
	private void triggerWaveCompleted(ServerLevel level, Raider raider, boolean removeFromTotalHealth, CallbackInfo ci) {
		if (groupsSpawned == 0 || getTotalRaidersAlive() > 0) {
			return;
		}
		Set<ServerPlayer> players = new LinkedHashSet<>(raidEvent.getPlayers());
		for (UUID hero : heroesOfTheVillage) {
			if (level.getPlayerByUUID(hero) instanceof ServerPlayer player) {
				players.add(player);
			}
		}
		for (ServerPlayer player : players) {
			LoveTropicsCriteria.RAID_WAVE_COMPLETED.get().trigger(player, groupsSpawned);
		}
	}
}
