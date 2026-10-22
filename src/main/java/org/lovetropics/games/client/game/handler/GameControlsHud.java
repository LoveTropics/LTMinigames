package org.lovetropics.games.client.game.handler;

import net.minecraft.ChatFormatting;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import org.lovetropics.games.LoveTropics;
import org.lovetropics.games.client.LTKeybinds;
import org.lovetropics.games.client.game.bingo.BingoBoardScreen;
import org.lovetropics.games.client.game.modifier.ModifiersScreen;
import org.lovetropics.games.client.game.shop.ShopScreen;
import org.lovetropics.games.common.content.bingo.Bingo;
import org.lovetropics.games.common.content.mcsr.McsrTexts;
import org.lovetropics.games.common.core.game.client_state.instance.ModifiersClientState;
import org.lovetropics.games.common.core.game.client_state.instance.ShopClientState;

import java.util.ArrayList;
import java.util.List;

/// Reminds players of the keys that open the bingo board, the shop and the rules, to the right of the hotbar
@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public final class GameControlsHud {
	private static final int HALF_HOTBAR_W = 91;
	private static final int HOTBAR_GAP = 6;
	/// Room for the offhand slot and the attack indicator, when they are drawn to the right of the hotbar
	private static final int OFFHAND_W = 29;
	private static final int ATTACK_INDICATOR_W = 24;
	private static final int LINE_SPACING = 10;
	private static final int BOTTOM_MARGIN = 3;

	@SubscribeEvent
	static void registerLayers(RegisterGuiLayersEvent event) {
		event.registerAboveAll(Identifier.fromNamespaceAndPath(LoveTropics.ID, "game_controls"), (graphics, deltaTracker) -> {
			Minecraft minecraft = Minecraft.getInstance();
			LocalPlayer player = minecraft.player;
			Screen screen = minecraft.gui.screen();
			if (player == null || minecraft.gui.hud.isHidden() || screen instanceof BingoBoardScreen || screen instanceof ShopScreen || screen instanceof ModifiersScreen) {
				return;
			}
			List<Component> lines = getLines();
			if (!lines.isEmpty()) {
				draw(graphics, minecraft.font, lines, getLeft(minecraft, player, graphics.guiWidth()));
			}
		});
	}

	private static List<Component> getLines() {
		List<Component> lines = new ArrayList<>();
		if (GameBingoHandler.board() != null) {
			lines.add(Bingo.OPEN_BOARD_HINT.apply(LTKeybinds.EXPAND_BINGO_BOARD.getTranslatedKeyMessage()));
		}
		ShopClientState shop = GameShopHandler.shop();
		if (shop != null) {
			lines.add(McsrTexts.OPEN_SHOP_HINT.apply(LTKeybinds.OPEN_SHOP.getTranslatedKeyMessage())
					.append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
					.append(McsrTexts.TROPICOINS.apply(shop.balance()).withStyle(ChatFormatting.GOLD)));
		}
		ModifiersClientState modifiers = GameModifiersHandler.modifiers();
		if (modifiers != null && !modifiers.entries().isEmpty()) {
			lines.add(McsrTexts.OPEN_RULES_HINT.apply(LTKeybinds.OPEN_RULES.getTranslatedKeyMessage()));
		}
		return lines;
	}

	/// Just right of the hotbar, and of whatever vanilla draws on that side of it
	private static int getLeft(Minecraft minecraft, LocalPlayer player, int screenWidth) {
		int left = screenWidth / 2 + HALF_HOTBAR_W + HOTBAR_GAP;
		HumanoidArm offhandArm = player.getMainArm().getOpposite();
		if (offhandArm == HumanoidArm.RIGHT && !player.getOffhandItem().isEmpty()) {
			left += OFFHAND_W;
		}
		if (offhandArm == HumanoidArm.LEFT && minecraft.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR) {
			left += ATTACK_INDICATOR_W;
		}
		return left;
	}

	/// The last line lines up with the bottom of the hotbar, and the others stack up above it
	private static void draw(GuiGraphicsExtractor graphics, Font font, List<Component> lines, int left) {
		int bottom = graphics.guiHeight() - BOTTOM_MARGIN - font.lineHeight;
		for (int i = 0; i < lines.size(); i++) {
			int y = bottom - (lines.size() - 1 - i) * LINE_SPACING;
			graphics.text(font, lines.get(i), left, y, 0xFFFFFFFF, true);
		}
	}
}
