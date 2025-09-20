package com.lovetropics.minigames.common.core.game.util;

import net.minecraft.SharedConstants;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.concurrent.Executor;

/**
 * Schedule events for within a game
 */
public final class GameScheduler implements Executor {

	/**
	 * This is to avoid concurrent modification exceptions
	 */
	private final ArrayList<Task> newTasks = new ArrayList<>();
	private final ArrayList<Task> delayedTasks = new ArrayList<>();

	public void tick() {
		delayedTasks.addAll(newTasks);
		newTasks.clear();
		delayedTasks.removeIf(Task::tick);
	}

	@Override
	public void execute(Runnable command) {
		schedule(new DelayedTask(command, 0));
	}

	public Handle runAfterTicks(int ticks, Runnable task) {
		return schedule(new DelayedTask(task, ticks));
	}

	public Handle runAfterSeconds(float seconds, Runnable task) {
		return runAfterTicks(Mth.floor(seconds * SharedConstants.TICKS_PER_SECOND), task);
	}

	public Handle runPeriodic(int tickDelay, int interval, Runnable task) {
		return schedule(new PeriodicTask(task, tickDelay, interval));
	}

	private Handle schedule(Task task) {
		newTasks.add(task);
		return task;
	}

	public void clearAllEvents() {
		newTasks.clear();
		delayedTasks.clear();
	}

	private static class DelayedTask implements Task {
		public final Runnable consumer;
		public int ticks;
		private boolean canceled;

		public DelayedTask(Runnable consumer, int ticks) {
			this.consumer = consumer;
			this.ticks = ticks;
		}

		@Override
		public boolean tick() {
			if (canceled) {
				return true;
			}
			if (--ticks <= 0) {
				consumer.run();
				return true;
			}
			return false;
		}

		@Override
		public void cancel() {
			canceled = true;
		}
	}

	private static class PeriodicTask implements Task {
		private final Runnable task;
		private final int interval;
		private int ticks;
		private boolean canceled;

		public PeriodicTask(Runnable task, int ticks, int interval) {
			this.task = task;
			this.ticks = ticks;
			this.interval = interval;
		}

		@Override
        public boolean tick() {
			if (canceled) {
				return true;
			}
			if (--ticks <= 0) {
				ticks = interval;
				task.run();
			}
			return false;
		}

		@Override
		public void cancel() {
			canceled = true;
		}
	}

	public interface Handle {
		void cancel();
	}

	private interface Task extends Handle {
		boolean tick();
	}
}
