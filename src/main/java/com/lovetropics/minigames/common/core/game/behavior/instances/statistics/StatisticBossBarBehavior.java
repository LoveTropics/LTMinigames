package com.lovetropics.minigames.common.core.game.behavior.instances.statistics;

import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePlayerEvents;
import com.lovetropics.minigames.common.core.game.player.PlayerRole;
import com.lovetropics.minigames.common.core.game.state.statistics.StatisticKey;
import com.lovetropics.minigames.common.core.game.util.GameBossBar;
import com.lovetropics.minigames.common.core.game.util.GlobalGameWidgets;
import com.lovetropics.minigames.common.core.game.util.TemplatedText;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record StatisticBossBarBehavior<T>(
		StatisticKey<T> statistic,
		int maxValue,
		boolean reversed,
		TemplatedText title,
		BossEvent.BossBarColor color,
		BossEvent.BossBarOverlay style,
		Style valueStyle,
		boolean global
) implements IGameBehavior {
	public static final MapCodec<StatisticBossBarBehavior<?>> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			StatisticKey.CODEC.fieldOf("statistic").forGetter(StatisticBossBarBehavior::statistic),
			Codec.INT.optionalFieldOf("max_value", 1).forGetter(StatisticBossBarBehavior::maxValue),
			Codec.BOOL.optionalFieldOf("reversed", false).forGetter(StatisticBossBarBehavior::reversed),
			TemplatedText.CODEC.fieldOf("title").forGetter(StatisticBossBarBehavior::title),
			BossEvent.BossBarColor.CODEC.optionalFieldOf("color", BossEvent.BossBarColor.WHITE).forGetter(StatisticBossBarBehavior::color),
			BossEvent.BossBarOverlay.CODEC.optionalFieldOf("style", BossEvent.BossBarOverlay.PROGRESS).forGetter(StatisticBossBarBehavior::style),
			Style.Serializer.CODEC.optionalFieldOf("value_style", Style.EMPTY).forGetter(StatisticBossBarBehavior::valueStyle),
			Codec.BOOL.optionalFieldOf("global", false).forGetter(StatisticBossBarBehavior::global)
	).apply(i, StatisticBossBarBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) throws GameException {
		if (global) {
			registerGlobal(game, events);
		} else {
			registerPerPlayer(game, events);
		}
	}

	private void registerGlobal(IGamePhase game, EventRegistrar events) {
		GlobalGameWidgets widgets = GlobalGameWidgets.registerTo(game, events);
		GameBossBar bossBar = widgets.openBossBar(CommonComponents.EMPTY, color, style);
		events.listen(GamePhaseEvents.TICK, () ->
				updateBossBar(bossBar, game.statistics().global().get(statistic))
		);
	}

	private void registerPerPlayer(IGamePhase game, EventRegistrar events) {
		Map<UUID, GameBossBar> bossBars = new HashMap<>();
		events.listen(GamePlayerEvents.SET_ROLE, (player, role, lastRole) -> {
			if (lastRole == PlayerRole.PARTICIPANT) {
				GameBossBar bossBar = bossBars.remove(player.getUUID());
				if (bossBar != null) {
					bossBar.removePlayer(player);
				}
			}
			if (role == PlayerRole.PARTICIPANT) {
				GameBossBar bossBar = new GameBossBar(CommonComponents.EMPTY, color, style);
				updateBossBar(bossBar, game.statistics().forPlayer(player).get(statistic));
				bossBar.addPlayer(player);
				bossBars.put(player.getUUID(), bossBar);
			}
		});

		events.listen(GamePlayerEvents.REMOVE, player -> {
			GameBossBar bossBar = bossBars.remove(player.getUUID());
			if (bossBar != null) {
				bossBar.removePlayer(player);
			}
		});

		events.listen(GamePlayerEvents.TICK, player -> {
			GameBossBar bossBar = bossBars.get(player.getUUID());
			if (bossBar != null) {
				updateBossBar(bossBar, game.statistics().forPlayer(player).get(statistic));
			}
		});
	}

	private void updateBossBar(GameBossBar bossBar, @Nullable T value) {
		bossBar.setTitle(title.apply(Map.of(
				"value", Component.literal(value != null ? statistic.display(value) : "").withStyle(valueStyle)
		)));
		float progress = 0.0f;
		if (value instanceof Number number) {
			progress = Mth.clamp(number.floatValue() / maxValue, 0.0f, 1.0f);
		}
		if (reversed) {
			progress = 1.0f - progress;
		}
		bossBar.setProgress(progress);
	}
}
