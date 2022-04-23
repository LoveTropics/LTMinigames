package com.lovetropics.minigames.client.lobby.screen.game_config;

import com.lovetropics.minigames.client.screen.LayoutTree;
import com.lovetropics.minigames.client.screen.flex.Align;
import com.lovetropics.minigames.client.screen.flex.Axis;
import com.lovetropics.minigames.client.screen.flex.Box;
import com.lovetropics.minigames.client.screen.flex.Layout;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.network.chat.Component;

import java.util.EnumMap;

public class TextLabel implements Widget {
	private final Layout renderArea;
	private final Component text;
	private final Font fnt = Minecraft.getInstance().font;

	private final EnumMap<Axis, Align.Cross> alignment = new EnumMap<>(Axis.class);
	
	public TextLabel(LayoutTree ltree, int height, Component text, Align.Cross horizontalAlign, Align.Cross verticalAlign) {
		height = Math.max(height, fnt.lineHeight);
		int width = ltree.head().content().width();
		Box margin = new Box();
		int textWidth = fnt.width(text);
		if (horizontalAlign == Align.Cross.CENTER) {
			int spacing = Math.max((width - textWidth) / 2, 0);
			margin = margin.left(spacing).right(spacing);
		} else if (horizontalAlign == Align.Cross.END) {
			margin = margin.shift(width - textWidth, 0);
		}
		if (verticalAlign == Align.Cross.CENTER) {
			int spacing = Math.max((height - fnt.lineHeight) / 2, 0);
			margin = margin.top(spacing).bottom(spacing);
		} else if (verticalAlign == Align.Cross.END) {
			margin = margin.shift(0, height - fnt.lineHeight);
		}
		ltree.definiteChild(-1, height, new Box(), margin);
		this.renderArea = ltree.pop();
		this.text = text;
		ltree.pop();
	}

	@Override
	public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		this.renderArea.debugRender(matrixStack);
		fnt.draw(matrixStack, text, renderArea.content().left(), renderArea.content().top(), -1);
	}
}
