package com.lovetropics.minigames.common.core.game.datagen;

import com.lovetropics.minigames.common.core.game.config.GameConfig;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class GameProvider implements DataProvider {
	private final PackOutput output;
	protected final BehaviorFactory behaviors;
	protected final CompletableFuture<HolderLookup.Provider> registries;

	protected GameProvider(PackOutput output, BehaviorFactory behaviors, CompletableFuture<HolderLookup.Provider> registries) {
		this.output = output;
		this.behaviors = behaviors;
		this.registries = registries;
	}

	protected abstract void generate(GameGenerator generator, HolderLookup.Provider holderProvider);

	@Override
	public CompletableFuture<?> run(CachedOutput pOutput) {
		return registries.thenCompose(regs -> {
			final List<GameBuilder> builders = new ArrayList<>();
			generate(id -> {
				final var builder = new GameBuilder(id);
				builders.add(builder);
				return builder;
			}, regs);

			final var gamesProv = output.createPathProvider(PackOutput.Target.DATA_PACK, "games");
			return CompletableFuture.allOf(builders.stream()
					.map(builder -> {
						final var built = builder.build();
						final var path = gamesProv.json(built.id());
						return DataProvider.saveStable(pOutput, regs, GameConfig.codec(built.id()), built, path);
					})
					.toArray(CompletableFuture[]::new));
		});
	}

	@Override
	public String getName() {
		return "Game";
	}

	public interface GameGenerator {
		GameBuilder builder(ResourceLocation id);
	}
}
