package org.lovetropics.games.common.core.game.datagen;

import org.lovetropics.games.common.core.game.behavior.IGameBehavior;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class BehaviorProvider implements DataProvider {

	private final PackOutput output;
	protected final BehaviorFactory behaviors;
	protected final CompletableFuture<HolderLookup.Provider> registries;

	public BehaviorProvider(PackOutput output, BehaviorFactory behaviors, CompletableFuture<HolderLookup.Provider> registries) {
		this.output = output;
		this.behaviors = behaviors;
		this.registries = registries;
	}

	@Override
	public CompletableFuture<?> run(CachedOutput pOutput) {
		return registries.thenCompose(regs -> {
			PackOutput.PathProvider behProv = output.createPathProvider(PackOutput.Target.DATA_PACK, "behaviors");
			return CompletableFuture.allOf(behaviors.stream()
					.map(entry -> {
						IGameBehavior built = entry.getValue();
						Path path = behProv.json(entry.getKey());
						return DataProvider.saveStable(pOutput, regs, IGameBehavior.CODEC, built, path);
					})
					.toArray(CompletableFuture[]::new));
		});
	}

	@Override
	public String getName() {
		return "Behaviors";
	}
}
