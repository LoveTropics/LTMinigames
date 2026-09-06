package com.lovetropics.minigames.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import org.jspecify.annotations.Nullable;

public final class TrimmedText {
	public static final TrimmedText EMPTY = new TrimmedText(CommonComponents.EMPTY);

	private final Component text;

	private @Nullable FormattedCharSequence trimmedText;
	private int trimmedWidth = -1;

	private TrimmedText(Component text) {
		this.text = text;
	}

	public static TrimmedText of(Component text) {
		return new TrimmedText(text);
	}

	public Component text() {
		return text;
	}

	public FormattedCharSequence forWidth(Font font, int width) {
		FormattedCharSequence trimmed = trimmedText;
		if (trimmed == null || width != trimmedWidth) {
			trimmedText = trimmed = computeForWidth(font, width);
			trimmedWidth = width;
		}
		return trimmed;
	}

	public boolean isTrimmedForWidth(Font font, int width) {
		return font.width(text) > width;
	}

	private FormattedCharSequence computeForWidth(Font font, int width) {
		return Language.getInstance().getVisualOrder(font.substrByWidth(text, width));
	}
}
