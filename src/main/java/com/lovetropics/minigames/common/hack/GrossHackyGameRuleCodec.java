package com.lovetropics.minigames.common.hack;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.gamerules.GameRules;

/*
* IM SORRY FOR THE HACKS THAT I DID
* THANK YOU GameRuleRegistryFix
* WE CAN REMOVE THIS ONCE WE UPDATE THE ZIPS TO NEW FORMAT BUT THIS WORKS FOR NOW
* */
public class GrossHackyGameRuleCodec implements Codec<GameRules> {
	
	private static final Codec<GameRules> REAL_CODEC = GameRules.codec(FeatureFlags.VANILLA_SET);

	@Override
	public <T> DataResult<Pair<GameRules, T>> decode(DynamicOps<T> ops, T input) {
		CompoundTag tag = (CompoundTag) input;
		CompoundTag newTag = new CompoundTag();
		tag.forEach((s, oldValue) -> {
			switch (s) {
				case "doFireTick" -> {
					boolean doFireTick = Boolean.parseBoolean(oldValue.asString().orElse("true"));
					boolean allowFireTicksAwayFromPlayer = Boolean.parseBoolean(tag.getString("allowFireTicksAwayFromPlayer").orElse("true"));
					int fireSpreadRadius;
					if (!doFireTick) {
						fireSpreadRadius = 0;
					} else if (!allowFireTicksAwayFromPlayer) {
						fireSpreadRadius = 128;
					} else {
						fireSpreadRadius = -1;
					}

					if (fireSpreadRadius != 128) {
						newTag.putInt("minecraft:fire_spread_radius_around_player", fireSpreadRadius);
					}
				}
				case "allowEnteringNetherUsingPortals" -> bool(newTag, "minecraft:allow_entering_nether_using_portals", oldValue);
				case "announceAdvancements" -> bool(newTag, "minecraft:show_advancement_messages", oldValue);
				case "blockExplosionDropDecay" -> bool(newTag, "minecraft:block_explosion_drop_decay", oldValue);
				case "commandBlockOutput", "enableCommandBlocks", "commandBlocksEnabled" -> bool(newTag, "minecraft:command_blocks_work", oldValue);
				case "commandModificationBlockLimit" -> integer(newTag, "minecraft:max_block_modifications", oldValue);
				case "disableElytraMovementCheck" -> boolInverted(newTag, "minecraft:elytra_movement_check", oldValue);
				case "disablePlayerMovementCheck" -> boolInverted(newTag, "minecraft:player_movement_check", oldValue);
				case "disableRaids" -> boolInverted(newTag, "minecraft:raids", oldValue);
				case "doDaylightCycle" -> bool(newTag, "minecraft:advance_time", oldValue);
				case "doEntityDrops" -> bool(newTag, "minecraft:entity_drops", oldValue);
				case "doImmediateRespawn" -> bool(newTag, "minecraft:immediate_respawn", oldValue);
				case "doInsomnia" -> bool(newTag, "minecraft:spawn_phantoms", oldValue);
				case "doLimitedCrafting" -> bool(newTag, "minecraft:limited_crafting", oldValue);
				case "doMobLoot" -> bool(newTag, "minecraft:mob_drops", oldValue);
				case "doMobSpawning" -> bool(newTag, "minecraft:spawn_mobs", oldValue);
				case "doPatrolSpawning" -> bool(newTag, "minecraft:spawn_patrols", oldValue);
				case "doTileDrops" -> bool(newTag, "minecraft:block_drops", oldValue);
				case "doTraderSpawning" -> bool(newTag, "minecraft:spawn_wandering_traders", oldValue);
				case "doVinesSpread" -> bool(newTag, "minecraft:spread_vines", oldValue);
				case "doWardenSpawning" -> bool(newTag, "minecraft:spawn_wardens", oldValue);
				case "doWeatherCycle" -> bool(newTag, "minecraft:advance_weather", oldValue);
				case "drowningDamage" -> bool(newTag, "minecraft:drowning_damage", oldValue);
				case "enderPearlsVanishOnDeath" -> bool(newTag, "minecraft:ender_pearls_vanish_on_death", oldValue);
				case "fallDamage" -> bool(newTag, "minecraft:fall_damage", oldValue);
				case "fireDamage" -> bool(newTag, "minecraft:fire_damage", oldValue);
				case "forgiveDeadPlayers" -> bool(newTag, "minecraft:forgive_dead_players", oldValue);
				case "freezeDamage" -> bool(newTag, "minecraft:freeze_damage", oldValue);
				case "globalSoundEvents" -> bool(newTag, "minecraft:global_sound_events", oldValue);
				case "keepInventory" -> bool(newTag, "minecraft:keep_inventory", oldValue);
				case "lavaSourceConversion" -> bool(newTag, "minecraft:lava_source_conversion", oldValue);
				case "locatorBar" -> bool(newTag, "minecraft:locator_bar", oldValue);
				case "logAdminCommands" -> bool(newTag, "minecraft:log_admin_commands", oldValue);
				case "maxCommandChainLength" -> integer(newTag, "minecraft:max_command_sequence_length", oldValue);
				case "maxCommandForkCount" -> integer(newTag, "minecraft:max_command_forks", oldValue);
				case "maxEntityCramming" -> integer(newTag, "minecraft:max_entity_cramming", oldValue);
				case "minecartMaxSpeed" -> integer(newTag, "minecraft:max_minecart_speed", oldValue);
				case "mobExplosionDropDecay" -> bool(newTag, "minecraft:mob_explosion_drop_decay", oldValue);
				case "mobGriefing" -> bool(newTag, "minecraft:mob_griefing", oldValue);
				case "naturalRegeneration" -> bool(newTag, "minecraft:natural_health_regeneration", oldValue);
				case "playersNetherPortalCreativeDelay" -> integer(newTag, "minecraft:players_nether_portal_creative_delay", oldValue);
				case "playersNetherPortalDefaultDelay" -> integer(newTag, "minecraft:players_nether_portal_default_delay", oldValue);
				case "playersSleepingPercentage" -> integer(newTag, "minecraft:players_sleeping_percentage", oldValue);
				case "projectilesCanBreakBlocks" -> bool(newTag, "minecraft:projectiles_can_break_blocks", oldValue);
				case "pvp" -> bool(newTag, "minecraft:pvp", oldValue);
				case "randomTickSpeed" -> integer(newTag, "minecraft:random_tick_speed", oldValue);
				case "reducedDebugInfo" -> bool(newTag, "minecraft:reduced_debug_info", oldValue);
				case "sendCommandFeedback" -> bool(newTag, "minecraft:send_command_feedback", oldValue);
				case "showDeathMessages" -> bool(newTag, "minecraft:show_death_messages", oldValue);
				case "snowAccumulationHeight" -> integer(newTag, "minecraft:max_snow_accumulation_height", oldValue);
				case "spawnMonsters" -> bool(newTag, "minecraft:spawn_monsters", oldValue);
				case "spawnRadius" -> integer(newTag, "minecraft:respawn_radius", oldValue);
				case "spawnerBlocksEnabled" -> bool(newTag, "minecraft:spawner_blocks_work", oldValue);
				case "spectatorsGenerateChunks" -> bool(newTag, "minecraft:spectators_generate_chunks", oldValue);
				case "tntExplodes" -> bool(newTag, "minecraft:tnt_explodes", oldValue);
				case "tntExplosionDropDecay" -> bool(newTag, "minecraft:tnt_explosion_drop_decay", oldValue);
				case "universalAnger" -> bool(newTag, "minecraft:universal_anger", oldValue);
				case "waterSourceConversion" -> bool(newTag, "minecraft:water_source_conversion", oldValue);
			}
		});
		return REAL_CODEC.decode(ops, (T) newTag);
	}

	@Override
	public <T> DataResult<T> encode(GameRules input, DynamicOps<T> ops, T prefix) {
		return REAL_CODEC.encode(input, ops, prefix);
	}

	private static void bool(CompoundTag target, String key, Tag source) {
		source.asString().ifPresent(value -> target.putBoolean(key, Boolean.parseBoolean(value)));
	}

	private static void boolInverted(CompoundTag target, String key, Tag source) {
		source.asString().ifPresent(value -> target.putBoolean(key, !Boolean.parseBoolean(value)));
	}

	private static void integer(CompoundTag target, String key, Tag source) {
		source.asString().ifPresent(value -> {
			try {
				target.putInt(key, Integer.parseInt(value));
			} catch (NumberFormatException ignored) {
			}
		});
	}
}
