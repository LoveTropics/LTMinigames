package com.lovetropics.minigames.common.core.game.behavior.instances;

import com.lovetropics.lib.codec.MoreCodecs;
import com.lovetropics.minigames.common.content.MinigameTexts;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressChannel;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressHolder;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressionPeriod;
import com.lovetropics.minigames.common.core.game.util.GameBossBar;
import com.lovetropics.minigames.common.core.game.util.GameWidgets;
import com.lovetropics.minigames.common.core.game.util.TemplatedText;
import com.lovetropics.minigames.common.util.Util;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ProgressBarBehavior implements IGameBehavior {
	private static final int UPDATE_INTERVAL = SharedConstants.TICKS_PER_SECOND / 2;

	public static final MapCodec<ProgressBarBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ProgressChannel.CODEC.optionalFieldOf("channel", ProgressChannel.MAIN).forGetter(b -> b.channel),
			Entry.CODEC.listOf().fieldOf("entries").forGetter(b -> b.entries)
	).apply(i, ProgressBarBehavior::new));

	private final ProgressChannel channel;
	private final List<Entry> entries;

	@Nullable
	private GameBossBar bossBar;
	private ProgressHolder progression;

	public ProgressBarBehavior(ProgressChannel channel, List<Entry> entries) {
		this.channel = channel;
		this.entries = entries;
	}

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		progression = channel.getOrThrow(game);

		GameWidgets widgets = GameWidgets.getOrRegister(game, events);

		events.listen(GamePhaseEvents.TICK, () -> {
			if (game.ticks() % UPDATE_INTERVAL != 0) {
				return;
			}

			Entry entry = getActiveEntry();
			if (entry == null) {
				if (bossBar != null) {
					bossBar.close();
					bossBar = null;
				}
				return;
			}

			updateVisibleBossBar(game, widgets, entry);
		});
	}

	@Nullable
	private Entry getActiveEntry() {
		for (Entry entry : entries) {
			if (progression.is(entry.period())) {
				return entry;
			}
		}
		return null;
	}

	private void updateVisibleBossBar(IGamePhase game, GameWidgets widgets, Entry entry) {
		Component text = getTitle(game, entry);
		if (bossBar == null) {
			bossBar = widgets.openGlobalBossBar(text, entry.color, BossEvent.BossBarOverlay.PROGRESS);
		} else {
			bossBar.setTitle(text);
			bossBar.setStyle(entry.color, BossEvent.BossBarOverlay.PROGRESS);
		}
		float progress = progression.progressIn(entry.period());
		bossBar.setProgress(entry.reversed ? 1.0f - progress : progress);
	}

	private Component getTitle(IGamePhase game, Entry entry) {
		int seconds;
		if (entry.textSinceStart) {
			int startTime = entry.period.start().resolve(progression);
			seconds = (progression.time() - startTime) / SharedConstants.TICKS_PER_SECOND;
		} else {
			int endTime = entry.period.end().resolve(progression);
			seconds = Mth.positiveCeilDiv(endTime - progression.time(), SharedConstants.TICKS_PER_SECOND);
		}
		return entry.title.resolve(game, seconds);
	}

	private record Entry(
			ProgressionPeriod period,
			Title title,
			BossEvent.BossBarColor color,
			boolean reversed,
			boolean textSinceStart
	) {
		private static final Codec<BossEvent.BossBarColor> BOSS_BAR_COLOR_CODEC = MoreCodecs.stringVariants(BossEvent.BossBarColor.values(), BossEvent.BossBarColor::getName);

		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
				ProgressionPeriod.CODEC.fieldOf("period").forGetter(Entry::period),
				Title.CODEC.forGetter(Entry::title),
				BOSS_BAR_COLOR_CODEC.optionalFieldOf("color", BossEvent.BossBarColor.WHITE).forGetter(Entry::color),
				Codec.BOOL.optionalFieldOf("reversed", false).forGetter(Entry::reversed),
				Codec.BOOL.optionalFieldOf("text_since_start", false).forGetter(Entry::textSinceStart)
		).apply(i, Entry::new));
	}

	private sealed interface Title {
		MapCodec<Title> CODEC = Codec.mapEither(Description.CODEC, Template.CODEC).xmap(
				either -> either.map(Function.identity(), Function.identity()),
				title -> switch (title) {
					case Description description -> Either.left(description);
					case Template template -> Either.right(template);
				}
		);

		Component resolve(IGamePhase game, int seconds);
	}

	private record Description(Component description, boolean includeTime) implements Title {
		public static final MapCodec<Description> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				ComponentSerialization.CODEC.fieldOf("text").forGetter(Description::description),
				Codec.BOOL.optionalFieldOf("include_time", false).forGetter(Description::includeTime)
		).apply(i, Description::new));

		@Override
		public Component resolve(IGamePhase game, int seconds) {
			if (!includeTime) {
				return description;
			}
			return MinigameTexts.progressBarTime(description, seconds);
		}
	}

	private record Template(TemplatedText template) implements Title {
		public static final MapCodec<Template> CODEC = TemplatedText.CODEC.fieldOf("template").xmap(Template::new, Template::template);

		@Override
		public Component resolve(IGamePhase game, int seconds) {
			Component timeText = Component.literal(Util.formatMinutesSeconds(seconds)).withStyle(ChatFormatting.AQUA);
			Component gameNameText = game.definition().name().copy().withStyle(ChatFormatting.AQUA);
			return template.apply(Map.of("time", timeText, "game", gameNameText));
		}
	}
}
