package org.lovetropics.games.lobbies.client.manage.screen.player_list;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import org.jspecify.annotations.Nullable;
import org.lovetropics.games.client.screen.ClientPlayerInfo;
import org.lovetropics.games.lobbies.client.screen.TrimmedText;

import java.util.UUID;

final class PlayerRow {
	static final int HEIGHT = 20;

	private static final int FACE_SIZE = 16;
	private static final int PADDING = 2;

	private PlayerRow() {}

	static void extract(
			GuiGraphicsExtractor graphics, Font font,
			int left, int top, int width, int height,
			UUID uuid, TrimmedText name, @Nullable Component detail,
			boolean hovered, boolean selected, int actionsWidth
	) {
		int right = left + width;
		int bottom = top + height;
		int middle = top + height / 2;

		if (selected) {
			graphics.fill(left, top, right, bottom, 0xFF808080);
			graphics.fill(left + 1, top + 1, right - 1, bottom - 1, 0xFF000000);
		} else if (hovered) {
			graphics.fill(left, top, right, bottom, 0xFF202020);
		}

		int faceX = left + PADDING;
		PlayerFaceExtractor.extractRenderState(graphics, ClientPlayerInfo.getSkin(uuid), faceX, middle - FACE_SIZE / 2, FACE_SIZE);

		int textX = faceX + FACE_SIZE + PADDING * 2;
		int textY = middle - font.lineHeight / 2;
		int textRight = right - PADDING * 2 - actionsWidth;

		// the actions take the place of the detail
		if (detail != null && actionsWidth == 0) {
			int detailWidth = font.width(detail);
			graphics.text(font, detail, textRight - detailWidth, textY, CommonColors.WHITE);
			textRight -= detailWidth + PADDING * 2;
		}

		graphics.text(font, name.forWidth(font, Math.max(textRight - textX, 0)), textX, textY, CommonColors.WHITE);
	}
}
