package com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels;

import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DdrInput;
import net.minecraft.core.Holder;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DdrLevelInputHandler {
	private final int maxTickDeviation;
	private final Deque<TimedDdrInput> pendingInputs;

	public DdrLevelInputHandler(int maxTickDeviation, Holder<DdrLevel> level) {
		this.maxTickDeviation = maxTickDeviation;
		pendingInputs = level.value().ticks().long2ObjectEntrySet().stream()
				.map(entry -> new TimedDdrInput(entry.getLongKey(), entry.getValue()))
				.sorted(Comparator.comparingLong(TimedDdrInput::tick))
				.collect(Collectors.toCollection(ArrayDeque::new));
	}

	public Stream<TimedDdrInput> pendingInputs() {
		return pendingInputs.stream();
	}

	private int discardExpiredInputs(long currentTick) {
		int count = 0;
		while (!pendingInputs.isEmpty()) {
			TimedDdrInput input = pendingInputs.peekFirst();
			if (input.tick() < currentTick - maxTickDeviation) {
				pendingInputs.removeFirst();
				count++;
			} else {
				break;
			}
		}
		return count;
	}

	public Result handleInput(DdrInput input, long currentTick) {
		int expiredCount = discardExpiredInputs(currentTick);
		Result missResult = new Result(expiredCount, null);
		if (input.isEmpty()) {
			return missResult;
		}

		Iterator<TimedDdrInput> iterator = pendingInputs.iterator();
		while (iterator.hasNext()) {
			TimedDdrInput pendingInput = iterator.next();
			if (pendingInput.tick() > currentTick + maxTickDeviation) {
				return missResult;
			}

			if (pendingInput.input().overlaps(input)) {
				iterator.remove();

				int deviationTicks = (int) Math.abs(pendingInput.tick() - currentTick);
				Hit hit = new Hit(deviationTicks, pendingInput.input().equals(input));

				return new Result(expiredCount, hit);
			}
		}

		return missResult;
	}

	public record Result(
			int missedCount,
			@Nullable Hit hit
	) {
	}

	public record Hit(int deviationTicks, boolean fullMatch) {
	}
}
