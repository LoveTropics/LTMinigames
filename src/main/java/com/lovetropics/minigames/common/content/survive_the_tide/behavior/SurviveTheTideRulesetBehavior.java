package com.lovetropics.minigames.common.content.survive_the_tide.behavior;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameLivingEntityEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.behavior.instances.ImmediateRespawnBehavior;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressChannel;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressHolder;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressionPeriod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.TriState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;

public class SurviveTheTideRulesetBehavior implements IGameBehavior {
	public static final MapCodec<SurviveTheTideRulesetBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ProgressionPeriod.CODEC.fieldOf("safe_period").forGetter(c -> c.safePeriod),
			Codec.BOOL.optionalFieldOf("force_drop_items_on_death", true).forGetter(c -> c.forceDropItemsOnDeath)
	).apply(i, SurviveTheTideRulesetBehavior::new));

	private final ProgressionPeriod safePeriod;
	private final boolean forceDropItemsOnDeath;

	private ProgressHolder progression;

	public SurviveTheTideRulesetBehavior(final ProgressionPeriod safePeriod, final boolean forceDropItemsOnDeath) {
		this.safePeriod = safePeriod;
		this.forceDropItemsOnDeath = forceDropItemsOnDeath;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		progression = ProgressChannel.MAIN.getOrThrow(game);

		events.listen(GamePlayerEvents.DEATH, this::onPlayerDeath);
		events.listen(GamePlayerEvents.DAMAGE, this::onPlayerHurt);
		events.listen(GamePlayerEvents.ATTACK, this::onPlayerAttackEntity);

		events.listen(GameLivingEntityEvents.ENDER_PEARL_TELEPORT, (player, x, y, z, damage, callback) -> {
			callback.accept(0f); // Set ender pearl damage to 0
		});
	}

	private TriState onPlayerDeath(ServerPlayer player, DamageSource damageSource) {
		if (forceDropItemsOnDeath && player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
			ImmediateRespawnBehavior.destroyVanishingCursedItems(player.getInventory());
			player.getInventory().dropAll();
		}
		return TriState.DEFAULT;
	}

	private TriState onPlayerHurt(ServerPlayer player, DamageSource source, float amount) {
		if ((source.getEntity() instanceof ServerPlayer || source.is(DamageTypeTags.IS_PROJECTILE)) && progression.is(safePeriod)) {
			return TriState.FALSE;
		}
		return TriState.DEFAULT;
	}

	private TriState onPlayerAttackEntity(ServerPlayer player, Entity target) {
		if (target instanceof ServerPlayer && progression.is(safePeriod)) {
			return TriState.FALSE;
		}
		return TriState.DEFAULT;
	}


}
