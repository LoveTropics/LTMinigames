package com.lovetropics.minigames.common.content.survive_the_tide.behavior;

import com.lovetropics.minigames.common.core.entity.MinigameEntities;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLogicEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class SttWinLogicBehavior implements IGameBehavior {
	public static final MapCodec<SttWinLogicBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.BOOL.optionalFieldOf("spawn_lightning_bolts_on_finish", false).forGetter(c -> c.spawnLightningBoltsOnFinish),
			Codec.INT.optionalFieldOf("lightning_bolt_spawn_tick_rate", 60).forGetter(c -> c.lightningBoltSpawnTickRate)
	).apply(i, SttWinLogicBehavior::new));

	protected final boolean spawnLightningBoltsOnFinish;
	protected final int lightningBoltSpawnTickRate;
	protected boolean minigameEnded;

	public SttWinLogicBehavior(final boolean spawnLightningBoltsOnFinish, final int lightningBoltSpawnTickRate) {
		this.spawnLightningBoltsOnFinish = spawnLightningBoltsOnFinish;
		this.lightningBoltSpawnTickRate = lightningBoltSpawnTickRate;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		events.listen(GameLogicEvents.GAME_OVER, winner -> minigameEnded = true);

		events.listen(GamePhaseEvents.TICK, () -> checkForGameEndCondition(game, game.level()));
	}

	private void checkForGameEndCondition(final IGamePhase game, final Level world) {
		if (minigameEnded) {
			if (spawnLightningBoltsOnFinish) {
				spawnLightningBoltsEverywhere(game, world);
			}
		}
	}

	private void spawnLightningBoltsEverywhere(IGamePhase game, final Level world) {
		if (game.ticks() % lightningBoltSpawnTickRate == 0) {
			for (ServerPlayer player : game.participants()) {
				int xOffset = (7 + world.random.nextInt(5)) * (world.random.nextBoolean() ? 1 : -1);
				int zOffset = (7 + world.random.nextInt(5)) * (world.random.nextBoolean() ? 1 : -1);

				int posX = Mth.floor(player.getX()) + xOffset;
				int posZ = Mth.floor(player.getZ()) + zOffset;

				int posY = world.getHeight(Heightmap.Types.MOTION_BLOCKING, posX, posZ);

				LightningBolt lightning = MinigameEntities.QUIET_LIGHTNING_BOLT.get().create(world, EntitySpawnReason.EVENT);
				lightning.snapTo(new Vec3(posX + 0.5, posY, posZ + 0.5));
				lightning.setVisualOnly(true);

				world.addFreshEntity(lightning);
			}
		}
	}
}
