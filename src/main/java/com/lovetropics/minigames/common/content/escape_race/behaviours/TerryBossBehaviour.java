package com.lovetropics.minigames.common.content.escape_race.behaviours;

import com.lovetropics.minigames.common.content.escape_race.EscapeRace;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.GameBehaviorType;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.state.team.GameTeamKey;
import com.lovetropics.minigames.common.core.game.state.team.TeamState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2FloatArrayMap;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.util.Mth;
import net.minecraft.util.TriState;

import java.util.function.Supplier;

public final class TerryBossBehaviour implements IGameBehavior {

	public static final MapCodec<TerryBossBehaviour> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			EntityPredicate.CODEC.fieldOf("entity_predicate").forGetter(tbb -> tbb.entityPredicate),
			Codec.FLOAT.fieldOf("damage_amount").forGetter(tbb -> tbb.damageAmount),
			Codec.INT.optionalFieldOf("days_to_give", 1).forGetter(tbb -> tbb.daysToGive)
	).apply(i, TerryBossBehaviour::new));

	private final EntityPredicate entityPredicate;
	private final float damageAmount;
	private final int daysToGive;

	public TerryBossBehaviour(
			EntityPredicate entityPredicate,
			float damageAmount,
			int daysToGive) {
		this.entityPredicate = entityPredicate;
		this.damageAmount = damageAmount;
		this.daysToGive = daysToGive;
	}

	private final Object2FloatMap<GameTeamKey> teamDamageMap = new Object2FloatArrayMap<>();

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		TeamState teams = game.instanceState().getOrThrow(TeamState.KEY);
		events.listen(GamePlayerEvents.ATTACK_DAMAGE, (player, target, damage) -> {
			if (!entityPredicate.matches(player, target)) {
				return TriState.DEFAULT;
			}
			GameTeamKey team = teams.getTeamForPlayer(player);
			if (team == null) {
				return TriState.DEFAULT;
			}
			float newDamageValue = teamDamageMap.getFloat(team) + damage;
			int daysEarned = Mth.floor(newDamageValue / damageAmount);
			if (daysEarned > 0) {
				game.statistics().forTeam(team).incrementInt(StatisticKey.VACATION_DAYS, daysEarned);
				newDamageValue -= daysEarned * damageAmount;
			}
			teamDamageMap.put(team, newDamageValue);
			return TriState.DEFAULT;
		});
	}

	@Override
	public Supplier<? extends GameBehaviorType<?>> behaviorType() {
		return EscapeRace.TERRY_BOSS;
	}
}
