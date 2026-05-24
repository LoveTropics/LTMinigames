package com.lovetropics.minigames.client.screen;

import com.lovetropics.minigames.client.screen.flex.Box;
import com.lovetropics.minigames.client.screen.flex.Layout;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public final class FlexUi {
	public static void fill(Layout layout, GuiGraphicsExtractor graphics, int color) {
		fill(layout.background(), graphics, color);
	}

	public static void fill(Box box, GuiGraphicsExtractor graphics, int color) {
		graphics.fill(box.left(), box.top(), box.right(), box.bottom(), color);
	}

	public static Button createButton(Layout layout, Component title, Button.OnPress action) {
		Box background = layout.background();
		return Button.builder(title, action)
				.bounds(background.left(), background.top(), background.width(), background.height())
				.build();
	}

	public static EditBox createTextField(Layout layout, Font font, Component title) {
		Box background = layout.background();
		return new EditBox(font, background.left(), background.top(), background.width(), background.height(), title);
	}
}
