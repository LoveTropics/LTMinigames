package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.SpawnBuilder;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.GameActionList;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticsMap;
import com.lovetropics.minigames.common.core.game.util.TemplatedText;
import com.lovetropics.minigames.common.util.Util;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.WardenAi;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

public record ImmediateRespawnBehavior(
		Optional<PlayerRole> role,
		Optional<PlayerRole> respawnAsRole,
		Optional<TemplatedText> deathMessage,
		boolean dropInventory,
		GameActionList respawnAction,
		boolean spectateKiller,
		boolean clearKillTracker,
		Optional<StatisticKey<Integer>> livesStatistic
) implements IGameBehavior {
	public static final MapCodec<ImmediateRespawnBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			PlayerRole.CODEC.optionalFieldOf("role").forGetter(c -> c.role),
			PlayerRole.CODEC.optionalFieldOf("respawn_as").forGetter(c -> c.respawnAsRole),
			TemplatedText.CODEC.optionalFieldOf("death_message").forGetter(c -> c.deathMessage),
			Codec.BOOL.optionalFieldOf("drop_inventory", false).forGetter(c -> c.dropInventory),
			GameActionList.CODEC.optionalFieldOf("respawn_action", GameActionList.EMPTY).forGetter(c -> c.respawnAction),
			Codec.BOOL.optionalFieldOf("spectate_killer", true).forGetter(c -> c.spectateKiller),
			Codec.BOOL.optionalFieldOf("clear_kill_tracker", false).forGetter(c -> c.clearKillTracker),
			StatisticKey.INT_CODEC.optionalFieldOf("lives").forGetter(c -> c.livesStatistic)
	).apply(i, ImmediateRespawnBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		respawnAction.register(game, events);
		events.listen(GamePlayerEvents.DEATH, (player, source) -> onPlayerDeath(game, player, source));
	}

	private TriState onPlayerDeath(IGamePhase game, ServerPlayer player, DamageSource source) {
		PlayerRole playerRole = game.getRoleFor(player);
		if (playerRole == PlayerRole.SPECTATOR) {
			return respawnWithSameRole(game, player, playerRole);
		}

		if (livesStatistic.isPresent()) {
			StatisticsMap playerStatistics = game.statistics().forPlayer(player);
			int lives = playerStatistics.getInt(livesStatistic.get());
			int newLives = Math.max(lives - 1, 0);
			playerStatistics.set(livesStatistic.get(), newLives);
			if (newLives > 0) {
				return respawnWithSameRole(game, player, playerRole);
			}
		}

		destroyVanishingCursedItems(player.getInventory());

		player.level().getEntities(EntityType.WARDEN, LivingEntity::isAlive).forEach(warden -> {
			warden.clearAnger(player);
			WardenAi.setDigCooldown(warden);
		});

		if (dropInventory) {
			player.getInventory().dropAll();
		}

		if (role.isEmpty() || role.get() == playerRole) {
			respawnPlayer(game, player, playerRole, source);
			sendDeathMessage(game, player);

			return TriState.FALSE;
		}

		return TriState.DEFAULT;
	}

	private TriState respawnWithSameRole(IGamePhase game, ServerPlayer player, @Nullable PlayerRole playerRole) {
		SpawnBuilder spawn = new SpawnBuilder(player);
		game.invoker(GamePlayerEvents.SPAWN).onSpawn(player.getUUID(), spawn, playerRole);
		spawn.teleportAndApply(player);
		game.invoker(GamePlayerEvents.RESPAWN).onRespawn(player);
		return TriState.FALSE;
	}

	private void respawnPlayer(IGamePhase game, ServerPlayer player, @Nullable PlayerRole playerRole, DamageSource source) {
		if (respawnAsRole.isPresent()) {
			game.setPlayerRole(player, respawnAsRole.get());
			final ServerPlayer killer = Util.getKillerPlayer(player, source);
			if (spectateKiller && respawnAsRole.get() == PlayerRole.SPECTATOR && killer != null) {
				player.setCamera(killer);
			}
			game.invoker(GamePlayerEvents.RESPAWN).onRespawn(player);
		} else {
			SpawnBuilder spawn = new SpawnBuilder(player);
			game.invoker(GamePlayerEvents.SPAWN).onSpawn(player.getUUID(), spawn, playerRole);
			spawn.teleportAndApply(player);
			game.invoker(GamePlayerEvents.RESPAWN).onRespawn(player);
		}

		// Run only at the end of the current game tick, as the code that caused the damage might still have side-effects
		game.scheduler().runAfterTicks(0, () ->
				respawnAction.apply(game, ContextMap.EMPTY, ActionSubjects.ofPlayer(player))
		);

		if (clearKillTracker) {
			for (ServerPlayer otherPlayer : game.participants()) {
				if (otherPlayer.getKillCredit() == player) {
					otherPlayer.setLastHurtByPlayer(net.minecraft.util.Util.NIL_UUID, 0);
				}
			}
		}
	}

	private void sendDeathMessage(IGamePhase game, ServerPlayer player) {
		if (deathMessage.isPresent()) {
			Component message = deathMessage.get().apply(Map.of(
					"message", player.getCombatTracker().getDeathMessage(),
					"killed", player.getDisplayName()
			));
			game.allPlayers().sendMessage(message);
		}
	}

	public static void destroyVanishingCursedItems(Container inventory) {
		for (int i = 0; i < inventory.getContainerSize(); ++i) {
			if (EnchantmentHelper.has(inventory.getItem(i), EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) {
				inventory.removeItemNoUpdate(i);
			}
		}
	}
}
