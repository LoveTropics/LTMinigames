package org.lovetropics.games.common.core.game.persistent.behavior.parkour;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePhaseEvents;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.persistent.PersistentGame;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehavior;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehaviorType;
import org.lovetropics.games.common.core.game.persistent.PersistentGameBehaviors;
import org.lovetropics.games.common.core.game.region.Region;
import org.lovetropics.games.common.core.game.region.RegionQuery;
import org.lovetropics.games.common.util.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ParkourBehavior implements PersistentGameBehavior {
	public static final MapCodec<ParkourBehavior> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
	        RegionQuery.CODEC.fieldOf("default_checkpoint").forGetter(b -> b.defaultCheckpoint),
	        RegionQuery.CODEC.listOf().fieldOf("checkpoints").forGetter(b -> b.checkpoints)
	).apply(instance, ParkourBehavior::new));

	private final RegionQuery defaultCheckpoint;
	private final List<RegionQuery> checkpoints;
	private final Map<ServerPlayer, RegionQuery> lastCheckpoint = new HashMap<>();

	public ParkourBehavior(RegionQuery defaultCheckpoint, List<RegionQuery> checkpoints) {
		this.defaultCheckpoint = defaultCheckpoint;
		this.checkpoints = checkpoints;
	}

	@Override
	public void register(PersistentGame game, EventRegistrar events) {
		events.listen(GamePlayerEvents.ADD, player -> {
			if (!canPlayerPlay(player)) {
				return;
			}
			lastCheckpoint.put(player, defaultCheckpoint);
			player.getInventory().add(new ItemStack(Parkour.PARKOUR_TELEPORTER.asItem()));
		});

		events.listen(GamePlayerEvents.REMOVE, player -> {
			lastCheckpoint.remove(player);
			for (ItemStack st : player.getInventory()) {
				if (st.is(Parkour.PARKOUR_TELEPORTER.asItem())) {
					player.getInventory().removeItem(st);
				}
			}
		});

		events.listen(GamePhaseEvents.STOP, reason -> {
			for (ServerPlayer player : lastCheckpoint.keySet()) {
				for (ItemStack st : player.getInventory()) {
					if (st.is(Parkour.PARKOUR_TELEPORTER.asItem())) {
						player.getInventory().removeItem(st);
					}
				}
			}
		});

		events.listen(GamePlayerEvents.USE_ITEM, (player, hand) ->  {
			RegionQuery checkpoint = lastCheckpoint.get(player);
			if (checkpoint == null) {
				return InteractionResult.PASS;
			}
			ItemStack stack = player.getItemInHand(hand);
			if (stack.is(Parkour.PARKOUR_TELEPORTER)) {
				Region region = checkpoint.nullableOne(game);
				if (region != null) {
					Vec3 center = region.center();
					center = center.add(0, -region.size().getY() / 2.0, 0);
					player.teleportTo(center.x, center.y, center.z);
					Util.sendNotifySound(player, SoundEvents.ARROW_HIT_PLAYER, SoundSource.BLOCKS, 0.4F, 1.0F);
				}
			}

			return InteractionResult.PASS;
		});

		events.listen(GamePhaseEvents.TICK, () -> {
			for (Map.Entry<ServerPlayer, RegionQuery> e : lastCheckpoint.entrySet()) {
				ServerPlayer player = e.getKey();
				int lastIndex = checkpoints.indexOf(e.getValue());
				for (int i = 0; i < checkpoints.size(); i++) {
					RegionQuery checkpoint = checkpoints.get(i);
					if (checkpoint.intersects(game, player.level(), player.getBoundingBox()) && i > lastIndex) {
						player.sendSystemMessage(Component.literal("Reached checkpoint!"), true);
						e.setValue(checkpoint);
						Util.sendNotifySound(player, SoundEvents.ARROW_HIT_PLAYER, SoundSource.BLOCKS, 0.4F, 1.0F);
					}
				}
			}
		});
	}

	private boolean canPlayerPlay(ServerPlayer player) {
		return player.gameMode() != GameType.SPECTATOR;
	}

	@Override
	public Supplier<? extends PersistentGameBehaviorType<?>> type() {
		return PersistentGameBehaviors.PARKOUR;
	}
}
