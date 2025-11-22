package com.lovetropics.minigames.common.core.game.behavior.instances.action;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.lovetropics.minigames.common.core.game.GameException;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.behavior.IGameBehavior;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionSubjects;
import com.lovetropics.minigames.common.core.game.behavior.action.ActionTarget;
import com.lovetropics.minigames.common.core.game.behavior.event.EventRegistrar;
import com.lovetropics.minigames.common.core.game.behavior.event.GameActionEvents;
import com.lovetropics.minigames.common.core.game.behavior.event.GamePhaseEvents;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressChannel;
import com.lovetropics.minigames.common.core.game.state.progress.ProgressionPoint;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

public record SimpleDialogBehavior(
		Map<String, Speaker> speakers,
		Map<ProgressionPoint, Entry> entries,
		ActionTarget target,
		Optional<ProgressChannel> progressChannel
) implements IGameBehavior {
	public static final MapCodec<SimpleDialogBehavior> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.unboundedMap(Codec.STRING, Speaker.CODEC).fieldOf("speakers").forGetter(SimpleDialogBehavior::speakers),
			Codec.unboundedMap(ProgressionPoint.STRING_CODEC, Entry.CODEC).fieldOf("entries").forGetter(SimpleDialogBehavior::entries),
			ActionTarget.CODEC.optionalFieldOf("target", ActionTarget.PASS).forGetter(SimpleDialogBehavior::target),
			ProgressChannel.CODEC.optionalFieldOf("progress_channel").forGetter(SimpleDialogBehavior::progressChannel)
	).apply(i, SimpleDialogBehavior::new));

	@Override
	public void register(IGamePhase game, EventRegistrar events) {
		if (progressChannel.isPresent()) {
			ProgressChannel channel = progressChannel.get();

			List<Pair<BooleanSupplier, ResolvedEntry>> actions = new ArrayList<>();
			for (Map.Entry<ProgressionPoint, Entry> entry : entries.entrySet()) {
				String speakerId = entry.getValue().speaker;
				Speaker speaker = speakers.get(speakerId);
				if (speaker == null) {
					throw new GameException(Component.literal("No speaker in " + speakerId));
				}
				ResolvedEntry resolved = resolve(entry, speaker);
				actions.add(Pair.of(entry.getKey().createPredicate(game, channel), resolved));
			}

			events.listen(GamePhaseEvents.TICK, () -> actions.removeIf(entry -> {
				if (entry.getFirst().getAsBoolean()) {
					send(game, entry.getSecond(), ActionSubjects.EMPTY);
					return true;
				}
				return false;
			}));
		} else {
			record ScheduledEntry(ResolvedEntry entry, ActionSubjects<?> subjects) {
			}

			Long2ObjectMap<ResolvedEntry> entries = new Long2ObjectOpenHashMap<>();
			for (Map.Entry<ProgressionPoint, Entry> entry : this.entries.entrySet()) {
				String speakerId = entry.getValue().speaker;
				Speaker speaker = speakers.get(speakerId);
				if (speaker == null) {
					throw new GameException(Component.literal("No speaker in " + speakerId));
				}
				entries.put(entry.getKey().resolve(ProgressionPoint.NamedResolver.EMPTY), resolve(entry, speaker));
			}

			Multimap<Long, ScheduledEntry> scheduled = HashMultimap.create();
			events.listen(GameActionEvents.APPLY, (context, targets) -> {
				for (Long2ObjectMap.Entry<ResolvedEntry> entry : entries.long2ObjectEntrySet()) {
					scheduled.put(game.ticks() + 1 + entry.getLongKey(), new ScheduledEntry(entry.getValue(), targets));
				}
				return true;
			});

			events.listen(GamePhaseEvents.TICK, () -> {
				for (ScheduledEntry entry : scheduled.removeAll(game.ticks())) {
					send(game, entry.entry, entry.subjects);
				}
			});
		}
	}

	private void send(IGamePhase game, ResolvedEntry entry, ActionSubjects<?> subjects) {
		List<ServerPlayer> players = target.resolveTargets(game, subjects).asPlayers(game);
		for (ServerPlayer player : players) {
			entry.message.ifPresent(player::sendSystemMessage);
			entry.sound.ifPresent(sound ->
					PlaySoundAction.playToPlayer(player, sound.value(), SoundSource.PLAYERS, 1.0f, 1.0f)
			);
		}
	}

	private static ResolvedEntry resolve(Map.Entry<ProgressionPoint, Entry> entry, Speaker speaker) {
		Optional<Component> message = entry.getValue().line().map(line -> formatMessage(speaker, line));
		return new ResolvedEntry(message, entry.getValue().sound());
	}

	private static Component formatMessage(Speaker speaker, Component line) {
		return Component.translatable("%s %s\n", speaker.name.copy().append(":").withStyle(ChatFormatting.BOLD), line);
	}

	public record Speaker(
			Component name
	) {
		public static final Codec<Speaker> CODEC = RecordCodecBuilder.create(i -> i.group(
				ComponentSerialization.CODEC.fieldOf("name").forGetter(Speaker::name)
		).apply(i, Speaker::new));
	}

	public record Entry(
			String speaker,
			Optional<Component> line,
			Optional<Holder<SoundEvent>> sound
	) {
		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.fieldOf("speaker").forGetter(Entry::speaker),
				ComponentSerialization.CODEC.optionalFieldOf("line").forGetter(Entry::line),
				SoundEvent.CODEC.optionalFieldOf("sound").forGetter(Entry::sound)
		).apply(i, Entry::new));
	}

	private record ResolvedEntry(
			Optional<Component> message,
			Optional<Holder<SoundEvent>> sound
	) {
	}
}
