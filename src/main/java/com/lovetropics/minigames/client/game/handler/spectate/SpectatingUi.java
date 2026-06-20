package com.lovetropics.minigames.client.game.handler.spectate;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.screen.ClientPlayerInfo;
import com.lovetropics.minigames.common.core.game.util.GameTexts;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.TeamColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public final class SpectatingUi {
	private static final Component FREE_CAMERA_TEXT = GameTexts.Ui.FREE_CAMERA.copy().withStyle(ChatFormatting.ITALIC);

	private static final int FACE_SIZE = 16;
	private static final int ENTRY_PADDING = 2;
	private static final int ENTRY_WIDTH = FACE_SIZE + ENTRY_PADDING * 2;
	private static final int ENTRY_TAG_HEIGHT = 2;
	private static final int ENTRY_HEIGHT = ENTRY_WIDTH + ENTRY_TAG_HEIGHT;
	private static final int HIGHLIGHTED_ENTRY_HEIGHT = ENTRY_HEIGHT + ENTRY_TAG_HEIGHT;

	private static final int MAX_ENTRIES_ON_SCREEN = 16;

	private final SpectatingSession session;
	private List<Entry> entries;
	private final Object2ObjectMap<UUID, PlayerEvent> events = new Object2ObjectOpenHashMap<>();

	private int selectedEntryIndex;
	private int highlightedEntryIndex;

	private int scrollViewIndex;

	private double accumulatedScroll;

	SpectatingUi(SpectatingSession session) {
		this.session = session;
		entries = createEntriesFor(session.players);
	}

	@SubscribeEvent
	public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
		SpectatingSession session = ClientSpectatingManager.INSTANCE.session;
		Minecraft minecraft = Minecraft.getInstance();
		if (session == null || minecraft.gui.screen() != null || minecraft.player == null || !minecraft.player.isSpectator()) {
			return;
		}

		double delta = event.getScrollDeltaY();

		// Prevent adjusting the spectator fly speed
		event.setCanceled(true);

		if (!InputConstants.isKeyDown(minecraft.getWindow(), InputConstants.KEY_LCONTROL)) {
			session.ui.onScrollSelection(delta);
		} else {
			if (session.state.allowsZoom()) {
				session.ui.onScrollZoom(delta);
			} else {
				// Allow changing the fly speed
				event.setCanceled(false);
			}
		}
	}

	private void onScrollZoom(double delta) {
		session.targetZoom = Mth.clamp(session.targetZoom - delta * 0.05, 0.0, 1.0);
	}

	private void onScrollSelection(double delta) {
		if (accumulatedScroll != 0.0 && Math.signum(delta) != Math.signum(accumulatedScroll)) {
			accumulatedScroll = 0.0;
		}

		accumulatedScroll += delta;

		int scrollAmount = (int) accumulatedScroll;
		if (scrollAmount != 0) {
			scrollSelection(-Mth.clamp(scrollAmount, -1, 1));
		}
	}

	@SubscribeEvent
	public static void onKeyInput(InputEvent.Key event) {
		SpectatingSession session = ClientSpectatingManager.INSTANCE.session;
		Minecraft minecraft = Minecraft.getInstance();
		if (session == null || minecraft.gui.screen() != null || minecraft.player == null || !minecraft.player.isSpectator() || event.getAction() == GLFW.GLFW_RELEASE) {
			return;
		}

		int key = event.getKey();

		if (key == GLFW.GLFW_KEY_LEFT) {
			session.ui.scrollSelection(-1);
		} else if (key == GLFW.GLFW_KEY_RIGHT) {
			session.ui.scrollSelection(1);
		} else if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
			session.ui.selectEntry(session.ui.highlightedEntryIndex);
		}
	}

	@SubscribeEvent
	public static void onMouseInput(InputEvent.MouseButton.Post event) {
		SpectatingSession session = ClientSpectatingManager.INSTANCE.session;
		Minecraft minecraft = Minecraft.getInstance();
		if (session == null || minecraft.gui.screen() != null || minecraft.player == null || !minecraft.player.isSpectator() || event.getAction() == GLFW.GLFW_RELEASE) {
			return;
		}

		int key = event.getButton();
		if (key == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			session.ui.selectEntry(session.ui.highlightedEntryIndex);
		}
	}

	private void scrollSelection(int shift) {
		int newIndex = highlightedEntryIndex + shift;
		newIndex = Mth.clamp(newIndex, 0, entries.size() - 1);

		highlightedEntryIndex = newIndex;
		scrollTo(newIndex);
	}

	private void selectEntry(int index) {
		Entry entry = entries.get(index);

		session.applyState(entry.selectionState);

		selectedEntryIndex = index;
		highlightedEntryIndex = index;
		scrollTo(index);
	}

	private void scrollTo(int index) {
		int scrollViewStart = scrollViewStart();
		int scrollViewEnd = scrollViewEnd();
		if (index < scrollViewStart) {
			scrollViewIndex += index - scrollViewStart;
		} else if (index > scrollViewEnd) {
			scrollViewIndex += index - scrollViewEnd;
		}
	}

	private int scrollViewStart() {
		return scrollViewIndex;
	}

	private int scrollViewEnd() {
		return scrollViewIndex + scrollViewSize() - 1;
	}

	private int scrollViewSize() {
		int padding = ENTRY_WIDTH * 4;
		int availableWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth() - padding;
		return Mth.clamp(availableWidth / ENTRY_WIDTH, 1, MAX_ENTRIES_ON_SCREEN);
	}

	public static void registerOverlays(RegisterGuiLayersEvent event) {
		event.registerBelow(VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND, LoveTropics.id("minigame_spectator"), (graphics, deltaTracker) -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.gui.hud.isHidden() || minecraft.player == null || !minecraft.player.isSpectator()) {
				return;
			}
			SpectatingSession session = ClientSpectatingManager.INSTANCE.session;
			if (session != null) {
				session.ui.renderChasePlayerList(graphics);
			}
		});
	}

	private void renderChasePlayerList(GuiGraphicsExtractor graphics) {
		int viewStart = scrollViewStart();
		int viewEnd = scrollViewEnd();
		int width = Math.min(entries.size(), scrollViewSize()) * ENTRY_WIDTH;
		int left = (graphics.guiWidth() - width) / 2;
		int right = left + width;
		int bottom = graphics.guiHeight();

		Font font = Minecraft.getInstance().font;
		int textY = bottom - (ENTRY_HEIGHT + font.lineHeight) / 2;
		if (viewStart > 0) {
			graphics.text(font, "<", left - font.width("<") - 2, textY, CommonColors.WHITE);
		}
		if (viewEnd < entries.size() - 1) {
			graphics.text(font, ">", right + 2, textY, CommonColors.WHITE);
		}

		int x = left;
		for (int i = viewStart; i <= viewEnd && i < entries.size(); i++) {
			boolean selected = i == selectedEntryIndex;
			boolean highlighted = i == highlightedEntryIndex;
			Entry entry = entries.get(i);
			entry.render(graphics, x, bottom, selected, highlighted, events.get(entry.playerIcon));
			x += ENTRY_WIDTH;
		}
	}

	public void onPlayerActivity(UUID player, int color) {
		events.put(player, new PlayerEvent(System.currentTimeMillis(), color));
	}

	void updatePlayers(List<UUID> players) {
		Entry oldSelectedEntry = entries.get(selectedEntryIndex);
		Entry oldHighlightedEntry = entries.get(highlightedEntryIndex);

		entries = createEntriesFor(players);

		int newSelectedEntry = getSelectedEntryIndex(oldSelectedEntry.selectionState);
		newSelectedEntry = newSelectedEntry != -1 ? newSelectedEntry : 0;

		selectEntry(newSelectedEntry);

		int newHighlightedEntry = getSelectedEntryIndex(oldHighlightedEntry.selectionState);
		highlightedEntryIndex = newHighlightedEntry != -1 ? newHighlightedEntry : selectedEntryIndex;

		updateEventsMap(players);
	}

	void updateState(SpectatingState state) {
		int index = getSelectedEntryIndex(state);
		if (index != -1) {
			selectedEntryIndex = index;
		} else {
			selectEntry(0);
		}
	}

	private int getSelectedEntryIndex(SpectatingState state) {
		for (int i = 0; i < entries.size(); i++) {
			Entry entry = entries.get(i);
			if (entry.selectionState.equals(state)) {
				return i;
			}
		}

		return -1;
	}

	List<Entry> createEntriesFor(List<UUID> players) {
		List<Entry> entries = new ArrayList<>(players.size() + 1);
		entries.add(new Entry(Minecraft.getInstance().player.getUUID(), () -> FREE_CAMERA_TEXT, null, SpectatingState.FREE_CAMERA));

		for (UUID player : players) {
			Supplier<Component> name = () -> {
				GameProfile profile = ClientPlayerInfo.getPlayerProfile(player);
				return profile != null ? Component.literal(profile.name()) : CommonComponents.ELLIPSIS;
			};

			PlayerTeam team = getTeamFor(player);

			entries.add(new Entry(player, name, team.getColor().map(TeamColor::textColor).orElse(null), new SpectatingState.SelectedPlayer(player)));
		}

		return entries;
	}

	void updateEventsMap(List<UUID> players) {
		events.keySet().removeIf(id -> !players.contains(id));
	}

	@Nullable
	private static PlayerTeam getTeamFor(UUID playerId) {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (connection != null) {
			PlayerInfo player = connection.getPlayerInfo(playerId);
			return player != null ? player.getTeam() : null;
		}
		return null;
	}

	record Entry(UUID playerIcon, Supplier<Component> nameSupplier, @org.jspecify.annotations.Nullable TextColor tagColor, SpectatingState selectionState) {
		private static final int SELECTED_OUTLINE_COLOR = CommonColors.WHITE;
		private static final int HIGHLIGHTED_OUTLINE_COLOR = 0xa0000000;
		private static final int TAB_COLOR = 0xff404040;

		void render(GuiGraphicsExtractor graphics, int left, int screenBottom, boolean selected, boolean highlighted, @Nullable PlayerEvent lastEvent) {
			int top = screenBottom - (highlighted ? HIGHLIGHTED_ENTRY_HEIGHT : ENTRY_HEIGHT);
			int bottom = top + ENTRY_HEIGHT;
			int right = left + ENTRY_WIDTH;

			if (highlighted || selected) {
				graphics.fill(left, top, right, bottom, selected ? SELECTED_OUTLINE_COLOR : HIGHLIGHTED_OUTLINE_COLOR);
			}

			int color = tagColor != null ? ARGB.opaque(tagColor.getValue()) : 0xffa0a0a0;
			graphics.fill(left, bottom - ENTRY_TAG_HEIGHT, right, bottom, color);
			graphics.fill(left, bottom, right, screenBottom, TAB_COLOR);

			PlayerSkin skin = ClientPlayerInfo.getSkin(playerIcon);
			PlayerFaceExtractor.extractRenderState(graphics, skin, left + ENTRY_PADDING, top + ENTRY_PADDING, FACE_SIZE);

			long now = System.currentTimeMillis();
			if (lastEvent != null && (now - lastEvent.time) <= 500) {
				double fade = Mth.lerp((now - lastEvent.time) / 500.0d, .75d, 0d);
				int fadeAlpha = (int) (fade * 255);
				int colour = lastEvent.color | (fadeAlpha << 24);
				graphics.fill(left + ENTRY_PADDING, top + ENTRY_PADDING, right - ENTRY_PADDING, top + ENTRY_PADDING + FACE_SIZE, colour);
			}

			if (highlighted) {
				renderName(graphics, left, top, selected);
			}
		}

		private void renderName(GuiGraphicsExtractor graphics, int left, int top, boolean selected) {
			Font font = Minecraft.getInstance().font;
			Component name = nameSupplier.get();
			if (!selected) {
				name = GameTexts.Ui.CLICK_TO_SELECT.apply(name.copy().withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY);
			}

			int nameLeft = left + (ENTRY_WIDTH - font.width(name)) / 2;
			graphics.text(font, name, nameLeft, top - font.lineHeight - 1, CommonColors.WHITE);
		}
	}

	record PlayerEvent(long time, int color) {
	}
}
