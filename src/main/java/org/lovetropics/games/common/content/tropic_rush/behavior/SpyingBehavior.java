package org.lovetropics.games.common.content.tropic_rush.behavior;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.common.content.shop.ShopTexts;
import org.lovetropics.games.common.content.tropic_rush.TropicRushTexts;
import org.lovetropics.games.common.core.game.IGamePhase;
import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import org.lovetropics.games.common.core.game.behavior.event.EventRegistrar;
import org.lovetropics.games.common.core.game.behavior.event.GamePlayerEvents;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;
import org.lovetropics.games.common.core.game.client_state.instance.ShopClientState;
import org.lovetropics.games.common.core.game.client_state.instance.SpyingClientState;
import org.lovetropics.games.common.core.game.state.GameStateKey;
import org.lovetropics.games.common.core.game.state.GameStateMap;
import org.lovetropics.games.common.core.game.state.IGameState;
import org.lovetropics.games.common.core.game.state.statistics.StatisticKey;
import org.lovetropics.games.common.core.item.MinigameDataComponents;
import org.lovetropics.games.common.core.network.spying.ClientboundOpenSpyingScreenPacket;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/// Lets players spy on other participants for a few time with a spying item either on their inventory, or through their eyes.
public final class SpyingBehavior implements IGameBehavior, IGameState {
	public static final MapCodec<SpyingBehavior> CODEC = MapCodec.unit(SpyingBehavior::new);
	public static final GameStateKey<SpyingBehavior> KEY = GameStateKey.create("Spying");

	private static final String SELECTOR = "spying";
	private static final int DURATION = 60 * SharedConstants.TICKS_PER_SECOND;
	private static final int REFRESH_INTERVAL = 10;

	private final Map<UUID, Watch> watches = new HashMap<>();
	private IGamePhase game;

	@Override
	public void registerState(IGamePhase game, GameStateMap phaseState, GameStateMap instanceState) {
		instanceState.register(KEY, this);
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		this.game = game;
		events.listen(GamePlayerEvents.USE_ITEM, (player, hand) -> {
			if (!isSpyingItem(player.getItemInHand(hand))) {
				return InteractionResult.PASS;
			}
			List<ShopClientState.Target> targets = getTargets(player);
			if (targets.isEmpty()) {
				player.sendSystemMessage(ShopTexts.NO_TARGETS.copy().withStyle(ChatFormatting.RED));
			} else {
				PacketDistributor.sendToPlayer(player, new ClientboundOpenSpyingScreenPacket(targets));
			}
			return InteractionResult.SUCCESS;
		});
		events.listen(GamePlayerEvents.TICK, this::tickWatch);
		events.listen(GamePlayerEvents.REMOVE, this::stopWatching);
		events.listen(GamePlayerEvents.SET_ROLE, (player, _, _) -> {
			if (watches.remove(player.getUUID()) != null) {
				GameClientState.removeFromPlayer(GameClientStateTypes.SPYING.get(), player);
			}
		});
	}

	private List<ShopClientState.Target> getTargets(ServerPlayer player) {
		return game.participants().stream()
				.filter(target -> target != player)
				.map(target -> new ShopClientState.Target(target.getUUID(), target.getDisplayName(), game.statistics().readForPlayer(target).getOr(StatisticKey.POINTS, 0)))
				.sorted(Comparator.comparingInt(ShopClientState.Target::points).reversed())
				.toList();
	}

	public void spy(ServerPlayer player, UUID targetId, boolean watch) {
		ItemStack item = getSpyingItem(player);
		ServerPlayer target = game.participants().getPlayerBy(targetId);
		if (item == null || player.getCooldowns().isOnCooldown(item) || target == null || target == player
				|| !game.participants().contains(player) || watches.containsKey(player.getUUID())) {
			return;
		}
		// Watching someone who is watching would only show where they are watching from
		if (watch && watches.containsKey(target.getUUID())) {
			player.sendSystemMessage(TropicRushTexts.SPYING_BUSY.apply(target.getDisplayName()).withStyle(ChatFormatting.RED));
			return;
		}

		UseCooldown cooldown = item.get(DataComponents.USE_COOLDOWN);
		if (cooldown != null) {
			cooldown.apply(item, player);
		}
		if (watch) {
			startWatching(player, target);
		} else {
			openInventory(player, target);
		}
	}

	private static boolean isSpyingItem(ItemStack stack) {
		return SELECTOR.equals(stack.get(MinigameDataComponents.SELECTOR));
	}

	private static @Nullable ItemStack getSpyingItem(ServerPlayer player) {
		for (InteractionHand hand : InteractionHand.values()) {
			if (isSpyingItem(player.getItemInHand(hand))) {
				return player.getItemInHand(hand);
			}
		}
		return null;
	}

	private void openInventory(ServerPlayer player, ServerPlayer target) {
		SimpleContainer contents = new SimpleContainer(45);
		player.openMenu(new SimpleMenuProvider((id, inventory, _) -> new ViewOnlyMenu(MenuType.GENERIC_9x5, id, inventory, contents, 5) {
			@Override
			public void broadcastChanges() {
				Inventory targetInventory = target.getInventory();
				for (int i = 0; i < targetInventory.getContainerSize(); i++) {
					contents.setItem(i, targetInventory.getItem(i));
				}
				super.broadcastChanges();
			}

			@Override
			public void removed(Player player) {
				super.removed(player);
				GameClientState.removeFromPlayer(GameClientStateTypes.SPYING.get(), (ServerPlayer) player);
			}
		}, TropicRushTexts.SPYING_INVENTORY.apply(target.getDisplayName()))).ifPresent(containerId -> {
			GameClientState.sendToPlayer(SpyingClientState.INSTANCE, player);
			game.scheduler().runAfterTicks(DURATION, () -> {
				if (player.containerMenu.containerId == containerId) {
					player.closeContainer();
				}
			});
		});
	}

	private void startWatching(ServerPlayer player, ServerPlayer target) {
		watches.put(player.getUUID(), new Watch(target, player.level(), player.position(), player.getYRot(), player.getXRot(), player.gameMode.getGameModeForPlayer(), game.ticks() + DURATION));
		player.setGameMode(GameType.SPECTATOR);
		follow(player, target);
		GameClientState.sendToPlayer(SpyingClientState.INSTANCE, player);
	}

	private void tickWatch(ServerPlayer player) {
		Watch watch = watches.get(player.getUUID());
		if (watch == null) {
			return;
		}

		ServerPlayer target = watch.target;
		if (player.getCamera() != target || game.ticks() >= watch.stopTime) {
			stopWatching(player);
		} else if (player.level() != target.level()) {
			follow(player, target);
		} else if (player.tickCount % REFRESH_INTERVAL == 0) {
			player.connection.send(new ClientboundSetCameraPacket(target));
			player.sendSystemMessage(TropicRushTexts.SPYING_WATCHING.apply(target.getDisplayName()), true);
		}
	}

	private static void follow(ServerPlayer player, ServerPlayer target) {
		player.teleportTo(target.level(), target.getX(), target.getY(), target.getZ(), Set.of(), target.getYRot(), target.getXRot(), true);
		player.setCamera(target);
	}

	private void stopWatching(ServerPlayer player) {
		Watch watch = watches.remove(player.getUUID());
		if (watch != null) {
			GameClientState.removeFromPlayer(GameClientStateTypes.SPYING.get(), player);
			player.setGameMode(watch.gameType);
			player.teleportTo(watch.level, watch.position.x, watch.position.y, watch.position.z, Set.of(), watch.yRot, watch.xRot, true);
		}
	}

	private record Watch(ServerPlayer target, ServerLevel level, Vec3 position, float yRot, float xRot, GameType gameType, long stopTime) {
	}

	private static class ViewOnlyMenu extends ChestMenu {
		ViewOnlyMenu(MenuType<?> type, int id, Inventory inventory, Container container, int rows) {
			super(type, id, inventory, container, rows);
		}

		@Override
		public void clicked(int slot, int button, ContainerInput input, Player player) {

		}

		@Override
		public ItemStack quickMoveStack(Player player, int slot) {
			return ItemStack.EMPTY;
		}
	}
}
