package com.lovetropics.minigames.client.render;

import com.lovetropics.lib.BlockBox;
import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.content.biodiversity_blitz.BiodiversityBlitz;
import com.lovetropics.minigames.common.content.biodiversity_blitz.client_state.ClientBbMobSpawnState;
import com.lovetropics.minigames.common.content.biodiversity_blitz.client_state.ClientBbScoreboardState;
import com.lovetropics.minigames.common.core.data.LoveTropicsAttachments;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.game.client_state.instance.HidePlayersState;
import com.lovetropics.minigames.common.core.game.client_state.instance.PointTagClientState;
import com.lovetropics.minigames.common.core.game.client_state.instance.StatisticOverlayState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.ARGB;
import net.minecraft.util.CommonColors;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.TriState;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

import org.jspecify.annotations.Nullable;
import java.util.List;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public class GameRendering {
	private static final ContextKey<PointTag> POINT_TAG_KEY = new ContextKey<>(LoveTropics.id("point_tag"));

	private static final int MOB_SPAWN_COLOR = ARGB.color(160, CommonColors.RED);

	private static final int SCOREBOARD_TEXT_ORDER = 1;

	@SubscribeEvent
	public static void render(SubmitCustomGeometryEvent event) {
		ClientBbMobSpawnState mobSpawnState = ClientGameStateManager.getOrNull(BiodiversityBlitz.MOB_SPAWN);
		ClientBbScoreboardState scoreboardState = ClientGameStateManager.getOrNull(BiodiversityBlitz.SCOREBOARD);
		if (mobSpawnState == null && scoreboardState == null) {
			return;
		}

		PoseStack poseStack = event.getPoseStack();
		Vec3 cameraPos = event.getLevelRenderState().cameraRenderState.pos;
		SubmitNodeCollector collector = event.getSubmitNodeCollector();

		if (mobSpawnState != null) {
			List<BlockBox> spawns = mobSpawnState.spawns();
			collector.submitCustomGeometry(poseStack, GameRenderTypes.TRANSLUCENT_NO_TEX, (pose, cons) -> {
				for (BlockBox box : spawns) {
					BlockPos min = box.min();
					BlockPos max = box.max();
					float x0 = (float) (min.getX() - cameraPos.x);
					float x1 = (float) (max.getX() + 1.0 - cameraPos.x);
					float y0 = (float) (min.getY() - cameraPos.y);
					float y1 = (float) (max.getY() + 1.0 - cameraPos.y);
					float z0 = (float) (min.getZ() - cameraPos.z);
					float z1 = (float) (max.getZ() + 1.0 - cameraPos.z);
					buildBox(cons, pose, x0, x1, y0, y1, z0, z1, MOB_SPAWN_COLOR);
				}
			});
		}

		if (scoreboardState != null) {
			renderScoreboardState(scoreboardState, poseStack, cameraPos, collector);
		}
	}

	private static void renderScoreboardState(ClientBbScoreboardState state, PoseStack matrices, Vec3 camera, SubmitNodeCollector collector) {
		AABB b = new AABB(state.start(), state.end());

		if (!state.side()) {
			collector.submitCustomGeometry(matrices, GameRenderTypes.TRANSLUCENT_NO_TEX_NO_OFFSET, (pose, cons) ->
					buildNorthFacing(cons, pose, (float) (b.minX - camera.x), (float) (b.maxX - camera.x), (float) (b.minY - camera.y), (float) (b.maxY - camera.y), (float) (b.minZ - camera.z), ARGB.color(30, CommonColors.BLACK), ARGB.color(60, CommonColors.BLACK)));
			return;
		}

		collector.submitCustomGeometry(matrices, GameRenderTypes.TRANSLUCENT_NO_TEX_NO_OFFSET, (pose, cons) ->
				buildEastFacing(cons, pose, (float) (b.minY - camera.y), (float) (b.maxY - camera.y), (float) (b.minZ - camera.z), (float) (b.maxZ - camera.z), (float) (b.minX - camera.x), ARGB.color(30, CommonColors.BLACK), ARGB.color(60, CommonColors.BLACK)));

		int diff = (int) (b.maxZ - b.minZ) * 25;

		matrices.pushPose();

		matrices.translate(b.minX - camera.x(), b.maxY - camera.y(), b.maxZ - camera.z());
		matrices.mulPose(Axis.XN.rotationDegrees(180));
		// TODO: this must be -Y or +Y based on map. Make it configurable!
		matrices.mulPose(Axis.YN.rotationDegrees(90));
		matrices.scale(0.04f, 0.04f, 0.04f);

		Font font = Minecraft.getInstance().font;

		Component header = state.header();
		drawComponent(matrices, collector, (diff - font.width(header)) / 2, 1, colorOf(header), header);

		List<Component> content = state.content();
		for (int i = 0; i < content.size(); i++) {
			Component comp = content.get(i);
			int di = i + 2 >> 1; // di = (i + 2) / 2;
			int mi = i & 1; // mi = i % 2;

			int voff = 10 * di + 1;

			int hoff = 1;
			if (mi == 1) {
				hoff = diff - font.width(comp) - 1;
			}

			drawComponent(matrices, collector, hoff, voff, colorOf(comp), comp);
		}

		matrices.popPose();
	}

	private static int colorOf(Component component) {
		TextColor color = component.getStyle().getColor();
		return color == null ? CommonColors.WHITE : ARGB.opaque(color.getValue());
	}

	private static void drawComponent(PoseStack matrices, SubmitNodeCollector collector, int hoff, int voff, int color, Component comp) {
		collector.order(SCOREBOARD_TEXT_ORDER).submitText(
				matrices,
				hoff,
				voff,
				comp.getVisualOrderText(),
				false,
				Font.DisplayMode.POLYGON_OFFSET,
				LightCoordsUtil.FULL_BRIGHT,
				color,
				0,
				0
		);
	}

	public static void buildBox(VertexConsumer buffer, PoseStack.Pose pose, float x1, float x2, float y1, float y2, float z1, float z2, int color) {
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
		event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
			@Override
			public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState renderState) {
				PointTagClientState pointTags = ClientGameStateManager.getOrNull(GameClientStateTypes.POINT_TAGS);
				Component points = pointTags != null ? pointTags.getPointsTextFor(avatar.getUUID()) : null;
				if (points != null) {
					ItemStackRenderState itemStackRenderState = new ItemStackRenderState();
					Minecraft.getInstance().getItemModelResolver().updateForTopItem(itemStackRenderState, pointTags.icon().create(), ItemDisplayContext.GUI, avatar.level(), null, 0);
					renderState.setRenderData(POINT_TAG_KEY, new PointTag(points, itemStackRenderState));
				}
			}
		});

		event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
			@Override
			public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState renderState) {
				if (avatar instanceof Player player) {
					if (player.hasData(LoveTropicsAttachments.HIGHLIGHT_COLOR)) {
						renderState.outlineColor = player.getData(LoveTropicsAttachments.HIGHLIGHT_COLOR);
					}
				}
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
		if (event.getEntityRenderState() instanceof AvatarRenderState avatarRenderState) {
			PointTag pointTag = avatarRenderState.getRenderData(POINT_TAG_KEY);
			if (avatarRenderState.nameTagAttachment != null && pointTag != null) {
				renderPlayerPoints(event, avatarRenderState, pointTag.icon, pointTag.points);
			}
		}
	}

	private static void renderPlayerPoints(RenderNameTagEvent.DoRender event, AvatarRenderState playerState, ItemStackRenderState icon, Component points) {
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
		poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().camera.rotation());
		poseStack.scale(-0.0625F * textScale, 0.0625F * textScale, 0.0625F * textScale);

		SubmitNodeCollector collector = event.getSubmitNodeCollector();
		int packedLight = event.getEntityRenderState().lightCoords;

		Font font = event.getEntityRenderer().getFont();

		float width = itemSize + spacing + font.width(points);
		float left = -width / 2.0F;

		poseStack.pushPose();
		poseStack.scale(-1.0F, -1.0F, 1.0F);

		float textX = left + itemSize + spacing;
		float textY = -font.lineHeight / 2.0F;
		collector.submitText(
				poseStack,
				textX,
				textY,
				points.getVisualOrderText(),
				false,
				Font.DisplayMode.NORMAL,
				packedLight,
				CommonColors.WHITE,
				0,
				playerState.outlineColor);
		poseStack.popPose();

		poseStack.pushPose();
		poseStack.translate(-(left + (itemSize / 2.0f)), 0.0F, 0.0F);
		poseStack.scale(itemSize, itemSize, -itemSize);
		icon.submit(poseStack, collector, packedLight, OverlayTexture.NO_OVERLAY, playerState.outlineColor);
		poseStack.popPose();

		poseStack.popPose();
	}

	@SubscribeEvent
	public static void renderPlayer(RenderPlayerEvent.Pre<AbstractClientPlayer> event) {
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
			ItemStackRenderState icon
	) {
	}

	private static StatisticOverlayState.@Nullable Ticker statisticTicker;

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
		event.registerBelow(VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND, LoveTropics.id("statistic"), (graphics, deltaTracker) -> {
			if (Minecraft.getInstance().gui.hud.isHidden()) {
				return;
			}
			if (statisticTicker != null) {
				renderStatisticOverlay(graphics, statisticTicker);
			}
		});
	}

	private static void renderStatisticOverlay(GuiGraphicsExtractor graphics, StatisticOverlayState.Ticker statisticOverlay) {
		final int padding = 3;
		final int itemSize = 16;

		Font font = Minecraft.getInstance().font;
		graphics.item(statisticOverlay.icon(), padding, padding);

		graphics.text(
				font,
				statisticOverlay.text(),
				padding + itemSize + padding,
				padding + 1 + (itemSize - font.lineHeight) / 2,
				CommonColors.WHITE
		);
	}
}
