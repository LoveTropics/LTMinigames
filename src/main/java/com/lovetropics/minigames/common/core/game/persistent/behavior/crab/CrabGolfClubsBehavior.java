package com.lovetropics.minigames.common.core.game.persistent.behavior.crab;

import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGame;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehavior;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehaviorType;
import com.lovetropics.minigames.common.core.game.persistent.PersistentGameBehaviors;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class CrabGolfClubsBehavior implements PersistentGameBehavior {
	public static final MapCodec<CrabGolfClubsBehavior> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
	        ItemStack.CODEC.listOf().fieldOf("clubs").forGetter(b -> b.stacks)
	).apply(instance, CrabGolfClubsBehavior::new));

	private final List<ItemStack> stacks;
	private final Map<ServerPlayer, List<ItemStack>> added = new HashMap<>();

	public CrabGolfClubsBehavior(List<ItemStack> stacks) {
		this.stacks = stacks;
	}

	@Override
	public void register(PersistentGame game, EventRegistrar events) {
		events.listen(CrabGolfEvents.START_GAME, (hole, player) -> {
			for (ItemStack stack : stacks) {
				ItemStack copy = stack.copy();
				player.getInventory().add(copy.copy());
				added.computeIfAbsent(player, k -> new ArrayList<>()).add(copy);
			}
		});

		events.listen(GamePhaseEvents.STOP, reason -> {
			for (Map.Entry<ServerPlayer, List<ItemStack>> e : added.entrySet()) {
				for (ItemStack stack : e.getValue()) {
					for (ItemStack st : e.getKey().getInventory()) {
						if (ItemStack.matches(stack, st)) {
							e.getKey().getInventory().removeItem(st);
						}
					}
				}
			}
		});

		events.listen(CrabGolfEvents.WIN_GAME, (hole, player, score) -> {
			List<ItemStack> remove = added.remove(player);
			if (remove != null) {

			}
		});
	}

	@Override
	public Supplier<? extends PersistentGameBehaviorType<?>> type() {
		return PersistentGameBehaviors.CRAB_GOLF_CLUBS;
	}
}
