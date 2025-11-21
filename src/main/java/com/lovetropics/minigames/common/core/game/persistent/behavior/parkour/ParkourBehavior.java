package com.lovetropics.minigames.common.core.game.persistent.behavior.parkour;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGame;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehavior;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehaviorType;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehaviors;
import com.lovetropics.minigames.common.core.map.MapRegions;
import com.lovetropics.minigames.common.core.map.SavedRegions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ParkourBehavior implements PersistentGameBehavior {
	public static final MapCodec<ParkourBehavior> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
	        Codec.STRING.fieldOf("default_checkpoint").forGetter(b -> b.defaultCheckpoint),
	        Codec.STRING.listOf().fieldOf("checkpoints").forGetter(b -> b.checkpoints)
	).apply(instance, ParkourBehavior::new));

	private final String defaultCheckpoint;
	private final List<String> checkpoints;
	private final Map<ServerPlayer, String> lastCheckpoint = new HashMap<>();
	private final Map<String, BlockBox> regions = new HashMap<>();

	public ParkourBehavior(String defaultCheckpoint, List<String> checkpoints) {
		this.defaultCheckpoint = defaultCheckpoint;
		this.checkpoints = checkpoints;
	}

	@Override
	public void register(PersistentGame game, EventRegistrar events) {
		events.listen(GamePhaseEvents.START, initiator -> {
			MapRegions regions = SavedRegions.get(game.level()).regions().compile();

			for (String s : checkpoints) {
				this.regions.put(s, regions.getAny(s));
			}
		});

		events.listen(GamePlayerEvents.ADD, player -> {
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
			String checkpoint = lastCheckpoint.get(player);
			if (checkpoint == null) {
				return InteractionResult.PASS;
			}
			ItemStack stack = player.getItemInHand(hand);
			if (stack.is(Parkour.PARKOUR_TELEPORTER)) {
				BlockBox region = regions.get(checkpoint);
				if (region != null) {
					Vec3 center = region.center();
					center = center.add(0, -region.size().getY() / 2.0, 0);
					player.teleportTo(center.x, center.y, center.z);
					game.level().playSound(null, player.blockPosition(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.BLOCKS, 0.4F, 1.0F);
				}
			}

			return InteractionResult.PASS;
		});

		events.listen(GamePhaseEvents.TICK, () -> {
			for (Map.Entry<ServerPlayer, String> e : lastCheckpoint.entrySet()) {
				ServerPlayer player = e.getKey();
				for (Map.Entry<String, BlockBox> ex : regions.entrySet()) {
					if (ex.getValue() == null) {
						continue;
					}

					if (ex.getValue().asAabb().intersects(player.getBoundingBox()) && !e.getValue().equals(ex.getKey())) {
						player.sendSystemMessage(Component.literal("Reached checkpoint!"), true);
						e.setValue(ex.getKey());
						game.level().playSound(null, player.blockPosition(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.BLOCKS, 0.4F, 1.0F);
					}
				}
			}
		});
	}

	@Override
	public Supplier<? extends PersistentGameBehaviorType<?>> type() {
		return PersistentGameBehaviors.PARKOUR;
	}
}
