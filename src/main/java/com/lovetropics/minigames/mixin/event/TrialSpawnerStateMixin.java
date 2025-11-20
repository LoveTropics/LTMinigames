package com.lovetropics.minigames.mixin.event;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.event.GameWorldEvents;
import com.lovetropics.minigames.common.core.game.impl.GamePhaseManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerStateData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(TrialSpawnerState.class)
public class TrialSpawnerStateMixin {
	@Shadow
	@Final
	private static int TIME_BETWEEN_EACH_EJECTION;

	@Inject(method = "tickAndGetNext", at = @At("HEAD"), cancellable = true)
	private void tickAndGetNext(BlockPos pos, TrialSpawner spawner, ServerLevel level, CallbackInfoReturnable<TrialSpawnerState> cir) {
		TrialSpawnerState self = (TrialSpawnerState) (Object) this;
		if (self != TrialSpawnerState.EJECTING_REWARD) {
			return;
		}

		IGamePhase game = GamePhaseManager.get().getGamePhaseAt(level, pos);
		if (game == null) {
			return;
		}

		TrialSpawnerStateData stateData = spawner.getStateData();
		// Matching conditions in base method
		if (!stateData.isReadyToEjectItems(level, TIME_BETWEEN_EACH_EJECTION, spawner.getTargetCooldownLength())) {
			return;
		}
		TrialSpawnerStateData.Packed packed = stateData.pack();
		if (packed.detectedPlayers().isEmpty()) {
			return;
		}

		if (game.invoker(GameWorldEvents.TRIAL_SPAWNER_EJECT_LOOT).onTrialSpawnerEjectLoot(pos, spawner)) {
			stateData.apply(new TrialSpawnerStateData.Packed(
					// Clear players, ends ejection state
					Set.of(),
					packed.currentMobs(),
					packed.cooldownEndsAt(),
					packed.nextMobSpawnsAt(),
					packed.totalMobsSpawned(),
					packed.nextSpawnData(),
					packed.ejectingLootTable()
			));
			cir.setReturnValue(self);
		}
	}
}
