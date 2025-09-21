package com.lovetropics.minigames.common.util;

import com.lovetropics.minigames.LoveTropics;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

@EventBusSubscriber(modid = LoveTropics.ID)
public final class Scheduler {
	private static final ConcurrentMap<Integer, Tick> TICKS = new ConcurrentHashMap<>();
	private static int time = 0;

	@SubscribeEvent
	public static void onTick(ServerTickEvent.Post event) {
		Scheduler.runTasks(event.getServer());
	}

	public static Tick atTime(int time) {
		return Scheduler.TICKS.computeIfAbsent(time, t -> new Tick());
	}

	public static Tick inTicks(int delay) {
		return Scheduler.atTime(Scheduler.time + delay);
	}

	public static Tick nextTick() {
		return inTicks(1);
	}

	private static void runTasks(MinecraftServer server) {
		int lastTime = Scheduler.time;
		int time = server.getTickCount();
		Scheduler.time = time;

		if (TICKS.isEmpty()) {
			return;
		}

		for (int i = lastTime + 1; i <= time; i++) {
			Tick tick = TICKS.remove(i);
			if (tick != null) {
				tick.run(server);
			}
		}
	}

	public static final class Tick implements Executor {
		private final List<Consumer<MinecraftServer>> tasks = new ArrayList<>();

		public <T> CompletableFuture<T> supply(Function<MinecraftServer, T> task) {
			CompletableFuture<T> future = new CompletableFuture<>();
			run(server -> {
				T result = task.apply(server);
				future.complete(result);
			});
			return future;
		}

		public void run(Consumer<MinecraftServer> task) {
			tasks.add(task);
		}

		void run(MinecraftServer server) {
			for (Consumer<MinecraftServer> task : tasks) {
				task.accept(server);
			}
		}

		@Override
		public void execute(Runnable command) {
			run(server -> command.run());
		}
	}
}
