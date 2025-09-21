package com.lovetropics.minigames.common.content.paint_party;

import com.lovetropics.minigames.common.content.paint_party.entity.PaintBallEntity;
import com.lovetropics.minigames.common.core.game.behavior.event.GameEventType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class PaintPartyEvents {
	public static final GameEventType<PaintBallHit> PAINTBALL_HIT = GameEventType.create(PaintBallHit.class, listeners -> (level, entity, pos) -> {
		for (PaintBallHit listener : listeners) {
			listener.onPaintBallHit(level, entity, pos);
		}
	});

	public interface PaintBallHit {
		void onPaintBallHit(Level level, PaintBallEntity entity, BlockPos pos);
	}
}
