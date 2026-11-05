package org.lovetropics.games.common.util.duck;

import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.server.PlayerAdvancements;

public interface PlayerAdvancementsExtension {
	<T extends CriterionTriggerInstance> void lt$addListener(Criterion<T> criterion, PlayerAdvancements.TriggerInstanceKey key);

	<T extends CriterionTriggerInstance> void lt$removeListener(CriterionTrigger<T> trigger, PlayerAdvancements.TriggerInstanceKey key);
}
