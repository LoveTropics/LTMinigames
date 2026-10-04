package org.lovetropics.games.common.content.paint_party;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.lovetropics.games.common.content.paint_party.entity.PaintBallEntity;
import org.lovetropics.games.common.core.game.behavior.event.GameEventType;

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
