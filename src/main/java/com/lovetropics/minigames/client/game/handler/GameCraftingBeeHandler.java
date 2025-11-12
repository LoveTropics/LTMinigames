package com.lovetropics.minigames.client.game.handler;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.content.crafting_bee.CraftingBeeTexts;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.game.client_state.instance.CraftingBeeCraftsClientState;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public class GameCraftingBeeHandler {
	private static int hintsRemaining;
	@Nullable
	private static Map<ResourceKey<Recipe<?>>, RecipeHintState> hintGrids;

	static final ClientGameStateHandler<CraftingBeeCraftsClientState> HANDLER = new ClientGameStateHandler<>() {
		@Override
		public void accept(CraftingBeeCraftsClientState state) {
			hintsRemaining = state.allowedHints();
			hintGrids = new HashMap<>();
		}

		@Override
		public void disable(CraftingBeeCraftsClientState state) {
			hintsRemaining = 0;
			hintGrids = null;
		}
	};

	private static final ResourceLocation ITEMS_BAR_SPRITE = LoveTropics.location("minigames/crafting_bee/items_bar");
	private static final ResourceLocation GRID_SPRITE = LoveTropics.location("minigames/crafting_bee/crafting_grid");

	@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
	public static class ModSubscriber {
		@SubscribeEvent
		static void onRegisterTooltips(final RegisterClientTooltipComponentFactoriesEvent event) {
			event.register(RecipeHintState.class, recipeHintState -> new ClientTooltipComponent() {
				@Override
				public int getHeight(Font font) {
					return 58;
				}

				@Override
				public int getWidth(Font font) {
					return 54;
				}

				@Override
				public void renderImage(Font font, int x, int y, int width, int height, GuiGraphics guiGraphics) {
					guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, GRID_SPRITE, x, y, 54, 54);
					for (int i = 0; i < recipeHintState.grid().size(); i++) {
						var ingredient = recipeHintState.grid.get(i);
						if (ingredient.isEmpty()) {
							continue;
						}

						var hintWidth = recipeHintState.width();

						guiGraphics.renderFakeItem(
								ingredient,
								x + 1 + 18 * (i % hintWidth),
								y + 1 + 18 * (i / hintWidth)
						);
					}
				}
			});
		}
	}

	@SubscribeEvent
	static void onGuiInit(ScreenEvent.Init.Post event) {
		if (getState() == null || !(event.getScreen() instanceof CraftingScreen screen)) {
			return;
		}

		event.addListener(new AbstractWidget(screen.getGuiLeft() + 22, screen.getGuiTop() - 21, 132, 21, Component.empty()) {
			@Override
			protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
				guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, ITEMS_BAR_SPRITE, this.getX(), this.getY(), 132, 21);
				var crafts = getState().crafts();
				for (int i = 0; i < crafts.size(); i++) {
					var craft = crafts.get(i);
					var x = getX() + 4 + i * 18;
					int y = getY() + 4;
					guiGraphics.renderFakeItem(craft.output(), x, y, 0);
					if (craft.done()) {
						guiGraphics.fill(x, y, x + 16, y + 16, 0xe5c6c6c6);
					}

					if (mouseX >= x && mouseX <= x + 16 && mouseY >= y && mouseY <= getY() + 20) {
						var hint = hintGrids.get(craft.recipeId());

						var tooltipLines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), craft.output()));
						if (craft.done()) {
							tooltipLines.set(0, tooltipLines.getFirst().copy().withStyle(ChatFormatting.GREEN));
						} else if (hint == null || hint.hiddenCount() > 0) {
							tooltipLines.add(CraftingBeeTexts.HINT);
							tooltipLines.add(CraftingBeeTexts.HINTS_LEFT.apply(hintsRemaining).withStyle(ChatFormatting.AQUA));
						}
						guiGraphics.setTooltipForNextFrame(Minecraft.getInstance().font, tooltipLines, Optional.<TooltipComponent>ofNullable(hint).filter($ -> !craft.done()), mouseX, mouseY);
					}
				}
			}

			@Override
			public void onClick(double mouseX, double mouseY, int button) {
				if (hintsRemaining <= 0) {
					return;
				}

				var crafts = getState().crafts();

				if (mouseY < getY() + 4 || mouseY > getY() + 4 + 16) {
					return;
				}
				if (mouseX < getX() + 4 || mouseX > getX() + 4 + (18 * crafts.size() - 1)) {
					return;
				}
				var index = (int) (mouseX - getX() - 4) / 18;

				var craft = crafts.get(index);
				if (craft.done()) {
					return;
				}

				var grid = hintGrids.computeIfAbsent(craft.recipeId(), k ->
						createHintState(craft.display(), SlotDisplayContext.fromLevel(Objects.requireNonNull(Minecraft.getInstance().level)))
				);

				if (grid.hiddenCount() == 0) {
					return;
				}

				IntList ingredientsToPick = new IntArrayList();
				for (int i = 0; i < grid.grid.size(); i++) {
					if (!grid.grid.get(i).isEmpty() && grid.hiddenSlots.get(i)) {
						ingredientsToPick.add(i);
					}
				}

				int filledGridAmount = grid.ingredientCount() - grid.hiddenCount();

				Collections.shuffle(ingredientsToPick);
				// Make sure that we never show the full recipe in just one hint
				var ingredientsToShow = new Random().nextInt(filledGridAmount == 0 ? Math.max(1, ingredientsToPick.size() - 1) : ingredientsToPick.size());

				for (int i = 0; i <= ingredientsToShow; i++) {
					grid.hiddenSlots().clear(ingredientsToPick.getInt(i));
				}

				hintsRemaining--;
			}

			@Override
			protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

			}
		});
	}

	@Nullable
	private static CraftingBeeCraftsClientState getState() {
		return ClientGameStateManager.getOrNull(GameClientStateTypes.CRAFTING_BEE_CRAFTS);
	}

	private static RecipeHintState createHintState(RecipeDisplay recipeDisplay, ContextMap contextMap) {
		if (recipeDisplay instanceof ShapedCraftingRecipeDisplay shaped) {
			List<ItemStack> ingredients = shaped.ingredients().stream()
					.map(slot -> resolveIngredient(slot, contextMap))
					.toList();
			BitSet hiddenSlots = new BitSet();
			for (int i = 0; i < ingredients.size(); i++) {
				if (!ingredients.get(i).isEmpty()) {
					hiddenSlots.set(i);
				}
			}
			return new RecipeHintState(ingredients, hiddenSlots, shaped.width(), shaped.height());
		} else if (recipeDisplay instanceof ShapelessCraftingRecipeDisplay shapeless) {
			List<ItemStack> ingredients = shapeless.ingredients().stream()
					.map(slot -> resolveIngredient(slot, contextMap))
					.toList();
			int width = Math.min(ingredients.size(), 3);
			int height = Mth.positiveCeilDiv(ingredients.size(), 3);
			BitSet hiddenSlots = new BitSet();
			hiddenSlots.set(0, ingredients.size());
			return new RecipeHintState(ingredients, hiddenSlots, width, height);
		}
		throw new UnsupportedOperationException("Unsupported recipe display: " + recipeDisplay);
	}

	private static ItemStack resolveIngredient(SlotDisplay slot, ContextMap contextMap) {
		List<ItemStack> candidates = slot.resolveForStacks(contextMap);
		if (candidates.isEmpty()) {
			return ItemStack.EMPTY;
		}
		for (ItemStack item : candidates) {
			// Prioritize vanilla items
			if (item.getItem().builtInRegistryHolder().key().location().getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) {
				return item;
			}
		}
		return candidates.getFirst();
	}

	public record RecipeHintState(
			List<ItemStack> grid,
			BitSet hiddenSlots,
			int width,
			int height
	) implements TooltipComponent {
		public int hiddenCount() {
			return hiddenSlots.cardinality();
		}

		public int ingredientCount() {
			return (int) grid.stream().filter(stack -> !stack.isEmpty()).count();
		}
	}
}
