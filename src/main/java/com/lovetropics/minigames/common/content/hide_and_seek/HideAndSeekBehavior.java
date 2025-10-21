package com.lovetropics.minigames.common.content.hide_and_seek;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.core.diguise.DisguiseType;
import com.lovetropics.minigames.common.core.diguise.PlayerDisguise;
import com.lovetropics.minigames.common.core.diguise.ServerPlayerDisguises;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.SpawnBuilder;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.player.PlayerSet;
import com.lovetropics.minigames.common.core.game.state.team.GameTeam;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

// TODO: clean up and split up
public final class HideAndSeekBehavior implements IGameBehavior {
	public static final MapCodec<HideAndSeekBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("spawn").forGetter(c -> c.spawnRegionKey),
			Codec.INT.fieldOf("initial_hide_seconds").forGetter(c -> c.initialHideSeconds),
			DisguiseType.CODEC.listOf().fieldOf("disguises").forGetter(c -> c.disguises),
			Creature.CODEC.listOf().fieldOf("creatures").forGetter(c -> c.creatures)
	).apply(i, HideAndSeekBehavior::new));

	private final String spawnRegionKey;
	private final int initialHideSeconds;
	private final List<DisguiseType> disguises;
	private final List<Creature> creatures;

	private IGamePhase game;
	private TeamState teams;
	private GameTeam hiders;
	private GameTeam seekers;

	private BlockBox spawnRegion;

	public HideAndSeekBehavior(String spawnRegionKey, int initialHideSeconds, List<DisguiseType> disguises, List<Creature> creatures) {
		this.spawnRegionKey = spawnRegionKey;
		this.initialHideSeconds = initialHideSeconds;
		this.disguises = disguises;
		this.creatures = creatures;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		this.game = game;

		teams = game.instanceState().getOrThrow(TeamState.KEY);

		if (disguises.isEmpty()) {
			throw new GameException(Component.literal("No possible disguises!"));
		}

		hiders = teams.getTeamByKey("hiders");
		seekers = teams.getTeamByKey("seekers");

		if (hiders == null || seekers == null) {
			throw new GameException(Component.literal("Missing hiders or seekers team!"));
		}

		spawnRegion = game.mapRegions().getOrThrow(spawnRegionKey);

		events.listen(GamePhaseEvents.START, this::start);

		events.listen(GamePlayerEvents.SPAWN, (playerId, spawn, role) -> spawnPlayer(spawn));
		events.listen(GamePlayerEvents.SET_ROLE, this::onPlayerSetRole);
		events.listen(GamePlayerEvents.ATTACK, this::onPlayerAttack);
		events.listen(GamePlayerEvents.DEATH, this::onPlayerDeath);
	}

	private void start() {
		int hideTicks = initialHideSeconds * 20;

		PlayerSet seekers = teams.getParticipantsForTeam(game, this.seekers.key());
		PlayerSet hiders = teams.getParticipantsForTeam(game, this.hiders.key());

		seekers.addPotionEffect(new MobEffectInstance(MobEffects.SLOWNESS, hideTicks, 255, true, false));
		seekers.addPotionEffect(new MobEffectInstance(MobEffects.BLINDNESS, hideTicks, 255, true, false));

		seekers.sendMessage(Component.literal("You will be let out to catch the hiders in " + initialHideSeconds + " seconds!"));
		hiders.sendMessage(Component.literal("You have " + initialHideSeconds + " seconds to hide from the seekers!"));

		for (ServerPlayer player : game.participants()) {
			if (teams.isOnTeam(player, this.hiders.key())) {
				setHider(player);
			} else {
				setSeeker(player);
			}
		}
	}

	private void onPlayerSetRole(ServerPlayer player, @Nullable PlayerRole role, @Nullable PlayerRole lastRole) {
		if (lastRole == PlayerRole.PARTICIPANT) {
			removeParticipant(player);
		}
	}

	private void respawnPlayer(ServerPlayer player) {
		BlockPos spawnPos = spawnRegion.sample(player.getRandom());
		player.teleportTo(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
	}

	private void spawnPlayer(SpawnBuilder spawn) {
		RandomSource random = game.level().getRandom();
		BlockPos spawnPos = spawnRegion.sample(random);
		spawn.teleportTo(game.level(), spawnPos, Direction.getRandom(random));
	}

	private TriState onPlayerAttack(ServerPlayer player, Entity target) {
		if (teams.isOnTeam(player, seekers.key()) && player.getMainHandItem().getItem() == HideAndSeek.NET.get()) {
			if (target instanceof ServerPlayer && teams.isOnTeam((ServerPlayer) target, hiders.key())) {
				return TriState.DEFAULT;
			}
		}
		return TriState.FALSE;
	}

	private TriState onPlayerDeath(ServerPlayer player, DamageSource damageSource) {
		GameTeamKey team = teams.getTeamForPlayer(player);
		if (team == hiders.key()) {
			onHiderCaptured(player);
		} else if (team == seekers.key()) {
			onSeekerDied(player);
		}
		return TriState.FALSE;
	}

	private void removeParticipant(ServerPlayer player) {
		if (teams.isOnTeam(player, hiders.key())) {
			PlayerDisguise disguise = PlayerDisguise.getOrNull(player);
			if (disguise != null) {
				disguise.clear();
			}
		}
	}

	private void setHider(ServerPlayer player) {
		DisguiseType disguise = nextDisguiseType(player.level().random);
		ServerPlayerDisguises.set(player, disguise);
	}

	private void setSeeker(ServerPlayer player) {
		player.getInventory().clearContent();
		ServerPlayerDisguises.clear(player);
		player.getInventory().add(new ItemStack(HideAndSeek.NET.get()));
	}

	private DisguiseType nextDisguiseType(RandomSource random) {
		DisguiseType disguise = Util.getRandom(disguises, random);
		DisguiseType.EntityConfig entity = disguise.entity();
		if (entity == null) {
			return disguise;
		}
		CompoundTag nbt = entity.nbt() != null ? entity.nbt().copy() : new CompoundTag();
		nbt.putBoolean("CustomNameVisible", false);
		return disguise.withEntity(entity.withNbt(nbt));
	}

	private void onHiderCaptured(ServerPlayer player) {
		teams.addPlayerTo(player, seekers.key());

		respawnPlayer(player);
		setSeeker(player);
	}

	private void onSeekerDied(ServerPlayer player) {
		respawnPlayer(player);
	}

	public record Creature(EntityType<?> entity, String[] regionKeys) {
		public static final Codec<Creature> CODEC = RecordCodecBuilder.create(i -> i.group(
				BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity").forGetter(c -> c.entity),
				MoreCodecs.arrayOrUnit(Codec.STRING, String[]::new).fieldOf("region").forGetter(c -> c.regionKeys)
		).apply(i, Creature::new));
	}
}
