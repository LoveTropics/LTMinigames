package com.lovetropics.minigames.client.render;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.content.biodiversity_blitz.BiodiversityBlitz;
import com.lovetropics.minigames.common.content.biodiversity_blitz.client_state.ClientBbMobSpawnState;
import com.lovetropics.minigames.common.content.biodiversity_blitz.client_state.ClientBbScoreboardState;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.game.client_state.instance.HidePlayersState;
import com.lovetropics.minigames.common.core.game.client_state.instance.PointTagClientState;
import com.lovetropics.minigames.common.core.game.client_state.instance.StatisticOverlayState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.util.TriState;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

import javax.annotation.Nullable;
import java.util.List;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public class GameRendering {
	private static final ContextKey<PointTag> POINT_TAG_KEY = new ContextKey<>(LoveTropics.location("point_tag"));

	private static final int MOB_SPAWN_COLOR = ARGB.color(160, CommonColors.RED);

	@SubscribeEvent
	public static void render(RenderLevelStageEvent.AfterWeather event) {
		PoseStack matrices = event.getPoseStack();
		Vec3 cameraPos = event.getCamera().getPosition();
		RenderBuffers buffers = Minecraft.getInstance().renderBuffers();
		VertexConsumer cons = buffers.bufferSource().getBuffer(GameRenderTypes.TRANSLUCENT_NO_TEX);

		ClientBbMobSpawnState state = ClientGameStateManager.getOrNull(BiodiversityBlitz.MOB_SPAWN);
		if (state != null) {
			for (BlockBox box : state.spawns()) {
				BlockPos min = box.min();
				BlockPos max = box.max();
				float x0 = (float) (min.getX() - cameraPos.x);
				float x1 = (float) (max.getX() + 1.0 - cameraPos.x);
				float y0 = (float) (min.getY() - cameraPos.y);
				float y1 = (float) (max.getY() + 1.0 - cameraPos.y);
				float z0 = (float) (min.getZ() - cameraPos.z);
				float z1 = (float) (max.getZ() + 1.0 - cameraPos.z);
				buildBox(cons, matrices, x0, x1, y0, y1, z0, z1, MOB_SPAWN_COLOR);
			}
		}

		ClientBbScoreboardState scoreboardState = ClientGameStateManager.getOrNull(BiodiversityBlitz.SCOREBOARD);
		if (scoreboardState != null) {
			renderScoreboardState(scoreboardState, matrices, cameraPos, buffers.bufferSource(), cons);
		}

		// Flush vertices
		buffers.bufferSource().endBatch();
	}

	private static void renderScoreboardState(ClientBbScoreboardState state, PoseStack matrices, Vec3 camera, MultiBufferSource.BufferSource buffers, VertexConsumer cons) {
		AABB b = new AABB(state.start(), state.end());
		PoseStack.Pose entry = matrices.last();

		if (state.side()) {
			buildEastFacing(cons, entry, (float) (b.minY - camera.y), (float) (b.maxY - camera.y), (float) (b.minZ - camera.z), (float) (b.maxZ - camera.z), (float) (b.minX - camera.x), ARGB.color(30, CommonColors.BLACK), ARGB.color(60, CommonColors.BLACK));

			int diff = (int) (b.maxZ - b.minZ) * 25;

			matrices.pushPose();

			matrices.translate(b.minX - camera.x(), b.maxY - camera.y(), b.maxZ - camera.z());
			matrices.mulPose(Axis.XN.rotationDegrees(180));
			// TODO: this must be -Y or +Y based on map. Make it configurable!
			matrices.mulPose(Axis.YN.rotationDegrees(90));
			matrices.scale(0.04f, 0.04f, 0.04f);
			int voff = 1;
			Component header = state.header();
			drawComponent(matrices, buffers, (diff - Minecraft.getInstance().font.width(header)) / 2, voff, header.getStyle().getColor().getValue(), header);

			List<Component> content = state.content();
			for (int i = 0; i < content.size(); i++) {
				Component comp = content.get(i);
				int di = i + 2 >> 1; // di = (i + 2) / 2;
				int mi = i & 1; // mi = i % 2;

				voff = 10 * di + 1;

				int hoff = 1;
				if (mi == 1) {
					hoff = diff - Minecraft.getInstance().font.width(comp) - 1;
				}

				TextColor col = comp.getStyle().getColor();
				drawComponent(matrices, buffers, hoff, voff, col == null ? CommonColors.WHITE : ARGB.opaque(col.getValue()), comp);
			}

			matrices.popPose();
		} else {
			buildNorthFacing(cons, entry, (float) (b.minX - camera.x), (float) (b.maxX - camera.x), (float) (b.minY - camera.y), (float) (b.maxY - camera.y), (float) (b.minZ - camera.z), ARGB.color(30, CommonColors.BLACK), ARGB.color(60, CommonColors.BLACK));
		}
	}

	private static void drawComponent(PoseStack matrices, MultiBufferSource buffers, int hoff, int voff, int color, Component comp) {
		Minecraft.getInstance().font.drawInBatch(
				comp,
				hoff,
				voff,
				color,
				false,
				matrices.last().pose(),
				buffers,
				Font.DisplayMode.POLYGON_OFFSET,
				0,
				LightTexture.FULL_BRIGHT
		);
	}

	public static void buildBox(VertexConsumer buffer, PoseStack poseStack, float x1, float x2, float y1, float y2, float z1, float z2, int color) {
		PoseStack.Pose pose = poseStack.last();

		buildNorthFacing(buffer, pose, x1, x2, y1, y2, z1, color, ARGB.transparent(color));
		buildNorthFacing(buffer, pose, x1, x2, y1, y2, z2, color, ARGB.transparent(color));

		buildEastFacing(buffer, pose, y1, y2, z1, z2, x1, color, ARGB.transparent(color));
		buildEastFacing(buffer, pose, y1, y2, z1, z2, x2, color, ARGB.transparent(color));
	}

	public static void buildNorthFacing(VertexConsumer buffer, PoseStack.Pose pose, float x1, float x2, float y1, float y2, float z, int colorBottom, int colorTop) {
		buffer.addVertex(pose, x1, y2, z).setColor(colorTop);
		buffer.addVertex(pose, x2, y2, z).setColor(colorTop);
		buffer.addVertex(pose, x2, y1, z).setColor(colorBottom);
		buffer.addVertex(pose, x1, y1, z).setColor(colorBottom);

		// Reverse - TODO: why? culling is disabled?

		buffer.addVertex(pose, x1, y2, z).setColor(colorTop);
		buffer.addVertex(pose, x1, y1, z).setColor(colorBottom);
		buffer.addVertex(pose, x2, y1, z).setColor(colorBottom);
		buffer.addVertex(pose, x2, y2, z).setColor(colorTop);
	}

	public static void buildEastFacing(VertexConsumer buffer, PoseStack.Pose pose, float y1, float y2, float z1, float z2, float x, int colorBottom, int colorTop) {
		buffer.addVertex(pose, x, y1, z2).setColor(colorBottom);
		buffer.addVertex(pose, x, y2, z2).setColor(colorTop);
		buffer.addVertex(pose, x, y2, z1).setColor(colorTop);
		buffer.addVertex(pose, x, y1, z1).setColor(colorBottom);

		// Reverse

		buffer.addVertex(pose, x, y1, z2).setColor(colorBottom);
		buffer.addVertex(pose, x, y1, z1).setColor(colorBottom);
		buffer.addVertex(pose, x, y2, z1).setColor(colorTop);
		buffer.addVertex(pose, x, y2, z2).setColor(colorTop);
	}

	@SubscribeEvent
	public static void onRegisterRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
		event.registerEntityModifier(PlayerRenderer.class, (player, state) -> {
			PointTagClientState pointTags = ClientGameStateManager.getOrNull(GameClientStateTypes.POINT_TAGS);
			Component points = pointTags != null ? pointTags.getPointsTextFor(player.getUUID()) : null;
			if (points != null) {
				state.setRenderData(POINT_TAG_KEY, new PointTag(points, pointTags.icon()));
			}
		});
	}

	@SubscribeEvent
	public static void canRenderPlayerName(RenderNameTagEvent.CanRender event) {
		if (event.getEntity() instanceof Player && ClientGameStateManager.getOrNull(GameClientStateTypes.HIDE_NAME_TAGS) != null) {
			event.setCanRender(TriState.FALSE);
		}
	}

	@SubscribeEvent
	public static void onRenderPlayerName(RenderNameTagEvent.DoRender event) {
		if (event.getEntityRenderState() instanceof PlayerRenderState playerState) {
			PointTag pointTag = playerState.getRenderData(POINT_TAG_KEY);
			if (playerState.nameTagAttachment != null && pointTag != null) {
				renderPlayerPoints(event, playerState, pointTag.icon, pointTag.points);
			}
		}
	}

	private static void renderPlayerPoints(RenderNameTagEvent.DoRender event, PlayerRenderState playerState, ItemStack icon, Component points) {
		if (playerState.isDiscrete) {
			return;
		}

		final Minecraft client = Minecraft.getInstance();
		final EntityRenderDispatcher renderDispatcher = client.getEntityRenderDispatcher();

		final float itemSize = 16.0F;
		final float spacing = 4.0f;
		final float textScale = 1.0F / 2.5F;

		PoseStack poseStack = event.getPoseStack();

		poseStack.pushPose();
		poseStack.translate(0.0, playerState.boundingBoxHeight + 0.75, 0.0);
		poseStack.mulPose(renderDispatcher.cameraOrientation());
		poseStack.scale(-0.0625F * textScale, 0.0625F * textScale, 0.0625F * textScale);

		MultiBufferSource buffer = event.getMultiBufferSource();
		int packedLight = event.getPackedLight();

		Font font = event.getEntityRenderer().getFont();
		ItemRenderer items = client.getItemRenderer();

		float width = itemSize + spacing + font.width(points);
		float left = -width / 2.0F;

		poseStack.pushPose();
		poseStack.scale(1.0F, -1.0F, 1.0F);

		float textX = left + itemSize + spacing;
		float textY = -font.lineHeight / 2.0F;
		font.drawInBatch(points, textX, textY, CommonColors.WHITE, false, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, packedLight);
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(left + (itemSize / 2.0f), 0.0F, 0.0F);
		poseStack.scale(itemSize, itemSize, -itemSize);
		items.renderStatic(icon, ItemDisplayContext.GUI, packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer, client.level, 0);
		poseStack.popPose();

		poseStack.popPose();
	}

	@SubscribeEvent
	public static void renderPlayer(RenderPlayerEvent.Pre event) {
		LocalPlayer localPlayer = Minecraft.getInstance().player;
		if (localPlayer == null) {
			return;
		}
		int entityId = event.getRenderState().id;
		HidePlayersState hidePlayers = ClientGameStateManager.getOrDefault(GameClientStateTypes.HIDE_PLAYERS, HidePlayersState.EMPTY);
		if (localPlayer.getId() != entityId && hidePlayers.playerIds().contains(entityId)) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void renderVehicleNameTag(RenderNameTagEvent.CanRender event) {
		if (!(event.getEntity().getControllingPassenger() instanceof Player player)) {
			return;
		}
		HidePlayersState hidePlayers = ClientGameStateManager.getOrDefault(GameClientStateTypes.HIDE_PLAYERS, HidePlayersState.EMPTY);
		if (player.isLocalPlayer() || !hidePlayers.playerIds().contains(player.getId())) {
			return;
		}
		event.setContent(player.getDisplayName());
		event.setCanRender(TriState.TRUE);
	}

	private record PointTag(
			Component points,
			ItemStack icon
	) {
	}

	@Nullable
	private static StatisticOverlayState.Ticker statisticTicker;

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		StatisticOverlayState state = ClientGameStateManager.getOrNull(GameClientStateTypes.STATISTIC_OVERLAY);
		if (state != null) {
			statisticTicker = state.tick(statisticTicker);
		} else {
			statisticTicker = null;
		}
	}

	@SubscribeEvent
	public static void registerOverlays(RegisterGuiLayersEvent event) {
		event.registerBelow(VanillaGuiLayers.DEBUG_OVERLAY, LoveTropics.location("statistic"), (graphics, deltaTracker) -> {
			if (Minecraft.getInstance().options.hideGui) {
				return;
			}
			if (statisticTicker != null) {
				renderStatisticOverlay(graphics, statisticTicker);
			}
		});
	}

	private static void renderStatisticOverlay(GuiGraphics graphics, StatisticOverlayState.Ticker statisticOverlay) {
		final int padding = 2;
		final int itemSize = 16;

		Font font = Minecraft.getInstance().font;
		graphics.renderItem(statisticOverlay.icon(), padding, padding);

		graphics.drawString(
				font,
				statisticOverlay.text(),
				padding + itemSize + padding,
				padding + 1 + (itemSize - font.lineHeight) / 2,
				CommonColors.WHITE
		);
	}
}
