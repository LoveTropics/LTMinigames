package com.lovetropics.minigames.common.core.game.persistent.behavior.crab;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLivingEntityEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGame;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehavior;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehaviorType;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehaviors;
import com.lovetropics.minigames.common.core.game.persistent.behavior.GameRegionBehavior;
import com.lovetropics.minigames.common.core.map.MapRegions;
import com.lovetropics.minigames.common.core.map.SavedRegions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class CrabGolfHoleBehavior implements PersistentGameBehavior {
	public static final MapCodec<CrabGolfHoleBehavior> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Codec.INT.fieldOf("hole").forGetter(b -> b.hole),
			Codec.STRING.fieldOf("main_region").forGetter(b -> b.mainRegionName),
			Codec.STRING.fieldOf("start_region").forGetter(b -> b.startRegionName),
			Codec.STRING.fieldOf("hole_region").forGetter(b -> b.holeRegionName),
			Codec.STRING.fieldOf("button_region").forGetter(b -> b.buttonRegionName)
	).apply(instance, CrabGolfHoleBehavior::new));

	private final int hole;
	private final String mainRegionName;
	private final String startRegionName;
	private final String holeRegionName;
	private final String buttonRegionName;

	private final List<BlockBox> mainRegions = new ArrayList<>();
	private BlockBox startRegion = null;
	private BlockBox holeRegion = null;
	private BlockBox buttonRegion = null;

	private final Map<ServerPlayer, LivingEntity> currentlyPlaying = new HashMap<>();

	public CrabGolfHoleBehavior(int hole, String mainRegionName, String startRegionName, String holeRegionName, String buttonRegionName) {
		this.hole = hole;
		this.mainRegionName = mainRegionName;
		this.startRegionName = startRegionName;
		this.holeRegionName = holeRegionName;
		this.buttonRegionName = buttonRegionName;
	}

	public boolean isOk() {
		return startRegion != null && holeRegion != null && buttonRegion != null;
	}

	@Override
	public void register(PersistentGame game, EventRegistrar events) {
		events.listen(GamePhaseEvents.START, initiator -> {
			MapRegions regions = SavedRegions.get(game.level()).regions().compile();
			mainRegions.addAll(regions.get(mainRegionName));
			startRegion = regions.getAny(startRegionName);
			holeRegion = regions.getAny(holeRegionName);
			buttonRegion = regions.getAny(buttonRegionName);
		});

		events.listen(GamePlayerEvents.USE_BLOCK, (player, level, pos, hand, hit) -> {
			if (isOk() && level.getBlockState(pos).is(BlockTags.BUTTONS) && buttonRegion.contains(pos)) {
				// Already playing?
				if (!currentlyPlaying.containsKey(player)) {
					game.invoker(CrabGolfEvents.START_GAME).onStart(hole, player);
				}
			}
			return InteractionResult.PASS;
		});

		events.listen(CrabGolfEvents.START_GAME, (hole, player) -> {
			if (isOk() && hole == this.hole) {
				// spawn crab
				Vec3 rcenter = startRegion.center();
				// move to floor
				Vec3 center = new Vec3(rcenter.x, rcenter.y - 0.5, rcenter.z);
				// setup nbt
				CompoundTag nbt = new CompoundTag();
				nbt.putString("id", "tropicraft:fiddler_crab");
				nbt.putBoolean("RollingDownTown", true);
				nbt.putBoolean("Silent", true);
				// TODO: health, attributes

				LivingEntity entity = (LivingEntity) EntityType.loadEntityRecursive(nbt, player.level(), EntitySpawnReason.COMMAND, (e) -> {
					e.snapTo(center.x, center.y, center.z, e.getYRot(), e.getXRot());
					return e;
				});

				player.level().tryAddFreshEntityWithPassengers(entity);

				currentlyPlaying.put(player, entity);
			}
		});

		// TODO: track score

		events.listen(GamePhaseEvents.TICK, () -> {
			if (!isOk()) {
				return;
			}
			if (currentlyPlaying.isEmpty()) {
				return;
			}

			for (Map.Entry<ServerPlayer, LivingEntity> e : new HashSet<>(currentlyPlaying.entrySet())) {
				LivingEntity entity = e.getValue();
				Vec3 velocity = entity.getDeltaMovement();
				if (Math.abs(velocity.x) < 0.01 && Math.abs(velocity.z) < 0.01) {
					if (holeRegion.asAabb().intersects(entity.getBoundingBox())) {
						// win!!
						entity.kill(game.level());
						currentlyPlaying.remove(e.getKey());

						game.invoker(CrabGolfEvents.WIN_GAME).onWin(hole, e.getKey(), -1);
					}
				}
			}
		});
	}

	@Override
	public Supplier<? extends PersistentGameBehaviorType<?>> type() {
		return PersistentGameBehaviors.CRAB_GOLF_HOLE;
	}
}
