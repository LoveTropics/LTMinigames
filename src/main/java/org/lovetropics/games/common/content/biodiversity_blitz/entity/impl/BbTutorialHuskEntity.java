package org.lovetropics.games.common.content.biodiversity_blitz.entity.impl;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.level.Level;
import org.lovetropics.games.common.content.biodiversity_blitz.entity.ai.DestroyCropGoal;
import org.lovetropics.games.common.content.biodiversity_blitz.plot.Plot;

// Husk that cannot ever attack the player
public class BbTutorialHuskEntity extends BbHuskEntity {
	public BbTutorialHuskEntity(EntityType<? extends Husk> type, Level level, Plot plot) {
		super(type, level, plot);
	}

	@Override
	protected void addBehaviourGoals() {
		goalSelector.addGoal(3, new DestroyCropGoal(this));
	}
}
