package org.lovetropics.games.mixin;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import org.lovetropics.games.common.core.advancement.CriteriaTracker;
import org.lovetropics.games.common.util.duck.PlayerAdvancementsExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin implements PlayerAdvancementsExtension {
	@Shadow
	private ServerPlayer player;

	@Shadow
	private <T extends CriterionTriggerInstance> void addListener(Criterion<T> typeAndInstance, PlayerAdvancements.TriggerInstanceKey criterion) {
		throw new AssertionError();
	}

	@Shadow
	private <T extends CriterionTriggerInstance> void removeListener(CriterionTrigger<T> type, PlayerAdvancements.TriggerInstanceKey criterion) {
		throw new AssertionError();
	}

	@Inject(method = "award", at = @At("HEAD"), cancellable = true)
	private void awardTrackedCriterion(AdvancementHolder holder, String criterion, CallbackInfoReturnable<Boolean> cir) {
		CriteriaTracker tracker = CriteriaTracker.get(holder);
		if (tracker != null) {
			cir.setReturnValue(tracker.award((PlayerAdvancements) (Object) this, player, criterion));
		}
	}

	@Inject(method = "load", at = @At("TAIL"))
	private void registerTrackedCriteria(ServerAdvancementManager manager, CallbackInfo ci) {
		CriteriaTracker.addListenersTo((PlayerAdvancements) (Object) this, player);
	}

	@Override
	public <T extends CriterionTriggerInstance> void lt$addListener(Criterion<T> criterion, PlayerAdvancements.TriggerInstanceKey key) {
		addListener(criterion, key);
	}

	@Override
	public <T extends CriterionTriggerInstance> void lt$removeListener(CriterionTrigger<T> trigger, PlayerAdvancements.TriggerInstanceKey key) {
		removeListener(trigger, key);
	}
}
