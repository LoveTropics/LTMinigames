package com.lovetropics.minigames.common.content.escape_race.ddr_machine.levels;

import com.lovetropics.minigames.common.content.escape_race.ddr_machine.DdrInput;
import net.minecraft.core.Holder;

import org.jspecify.annotations.Nullable;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;

public final class DdrLevelInputQueue {
	private final int maxTickDeviation;
	private final Deque<TimedDdrInput> pendingInputs;

	public DdrLevelInputQueue(int maxTickDeviation, Holder<DdrLevel> level) {
		this.maxTickDeviation = maxTickDeviation;
		pendingInputs = new ArrayDeque<>(level.value().inputs());
	}

	public Collection<TimedDdrInput> pendingInputs() {
		return Collections.unmodifiableCollection(pendingInputs);
	}

	public int discardExpiredInputs(long currentTick) {
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

	public void clearInputAt(long inputTick) {
		Iterator<TimedDdrInput> iterator = pendingInputs.iterator();
		while (iterator.hasNext()) {
			TimedDdrInput input = iterator.next();
			if (input.tick() < inputTick) {
				continue;
			}
			if (input.tick() == inputTick) {
				iterator.remove();
			}
			break;
		}
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
				Hit hit = new Hit(pendingInput.tick(), deviationTicks, pendingInput.input().equals(input));

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

	public record Hit(long hitTick, int deviationTicks, boolean fullMatch) {
	}
}
