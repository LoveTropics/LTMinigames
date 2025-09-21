package com.lovetropics.minigames.client;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.client.game.ClientGameStateManager;
import com.lovetropics.minigames.common.core.diguise.DisguiseType;
import com.lovetropics.minigames.common.core.diguise.PlayerDisguise;
import com.lovetropics.minigames.common.core.diguise.PlayerDisguiseBehavior;
import com.lovetropics.minigames.common.core.game.client_state.GameClientStateTypes;
import com.lovetropics.minigames.common.core.item.MinigameItems;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.scores.Team;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.function.Supplier;

@EventBusSubscriber(modid = LoveTropics.ID, value = Dist.CLIENT)
public final class ClientPlayerDisguises {
	private static final EquipmentSlot[] EQUIPMENT_SLOTS = EquipmentSlot.values();

	private static final LoadingCache<ResolvableProfile, Supplier<PlayerSkin>> SKIN_LOOKUP_CACHE = CacheBuilder.newBuilder()
			.expireAfterAccess(Duration.ofSeconds(15))
			.build(new CacheLoader<>() {
				@Override
				public Supplier<PlayerSkin> load(ResolvableProfile profile) {
					GameProfile gameProfile = profile.gameProfile();
					return Minecraft.getInstance().getSkinManager().lookupInsecure(gameProfile);
				}
			});

	private static final ContextKey<DisguiseRenderState> DISGUISE_KEY = new ContextKey<>(LoveTropics.location("disguise"));

	@SubscribeEvent
	public static void onRegisterRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
		event.registerEntityModifier((Class<? extends LivingEntityRenderer<?, ?, ?>>) (Class<?>) LivingEntityRenderer.class, (entity, state) -> {
			DisguiseRenderState disguise = extractDisguiseState(entity, state.partialTick);
			if (disguise != null) {
				state.setRenderData(DISGUISE_KEY, disguise);
			}
		});
	}

	@Nullable
	private static DisguiseRenderState extractDisguiseState(LivingEntity entity, float partialTicks) {
		PlayerDisguise disguise = PlayerDisguise.getOrNull(entity);
		if (disguise == null || !disguise.isDisguised()) {
			return null;
		}
		Entity disguiseEntity = disguise.entity();
		if (disguiseEntity == null) {
			return DisguiseRenderState.scaling(disguise.type().scale());
		}
		return extractDisguiseState(entity, partialTicks, disguiseEntity, disguise);
	}

	private static <E extends Entity> DisguiseRenderState extractDisguiseState(LivingEntity entity, float partialTicks, E disguiseEntity, PlayerDisguise disguise) {
		EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
		EntityRenderer<? super E, ?> renderer = entityRenderDispatcher.getRenderer(disguiseEntity);
		if (renderer == null) {
			return DisguiseRenderState.scaling(disguise.type().scale());
		}

		try {
			copyDisguiseState(disguiseEntity, entity);
			if (entity instanceof final Player player) {
				disguiseEntity.setCustomNameVisible(shouldShowName(entityRenderDispatcher, player));
			}

			return new DisguiseRenderState(
					renderer.createRenderState(disguiseEntity, partialTicks),
					disguise.type().scale()
			);
		} catch (Exception e) {
			disguise.clear();
			LoveTropics.LOGGER.error("Failed to capture player disguise state", e);
		}

		return DisguiseRenderState.scaling(disguise.type().scale());
	}

	@SubscribeEvent
	public static void onRenderPlayerPre(RenderLivingEvent.Pre<?, ?, ?> event) {
		DisguiseRenderState disguiseState = event.getRenderState().getRenderData(DISGUISE_KEY);
		if (disguiseState == null) {
			return;
		}

		EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
		PoseStack poseStack = event.getPoseStack();

		EntityRenderState disguiseEntityState = disguiseState.entityRenderState();
		float scale = disguiseState.scale();

		if (disguiseEntityState != null) {
			int capturedTransformState = PoseStackCapture.get(poseStack);

			try {
				MultiBufferSource bufferSource = event.getMultiBufferSource();
				int packedLight = event.getPackedLight();

				poseStack.pushPose();
				if (scale != 1.0f) {
					poseStack.scale(scale, scale, scale);
				}

				dispatcher.render(disguiseEntityState, 0.0, 0.0, 0.0, poseStack, bufferSource, packedLight);

				poseStack.popPose();
			} catch (Exception e) {
				LoveTropics.LOGGER.error("Failed to render player disguise", e);
				PoseStackCapture.restore(poseStack, capturedTransformState);
			}

			event.setCanceled(true);
		} else {
			poseStack.pushPose();
			if (scale != 1.0f) {
				poseStack.scale(scale, scale, scale);
			}
		}
	}

	@SubscribeEvent
	public static void onRenderPlayerPost(RenderLivingEvent.Post<?, ?, ?> event) {
		DisguiseRenderState disguiseState = event.getRenderState().getRenderData(DISGUISE_KEY);
		if (disguiseState == null) {
			return;
		}
		if (disguiseState.entityRenderState() != null || disguiseState.scale() != 1.0f) {
			event.getPoseStack().popPose();
		}
	}

	private static void copyDisguiseState(Entity disguise, LivingEntity entity) {
		disguise.setPos(entity.getX(), entity.getY(), entity.getZ());
		disguise.xo = entity.xo;
		disguise.yo = entity.yo;
		disguise.zo = entity.zo;

		disguise.setYRot(entity.getYRot());
		disguise.yRotO = entity.yRotO;
		disguise.setXRot(entity.getXRot());
		disguise.xRotO = entity.xRotO;

		disguise.setShiftKeyDown(entity.isShiftKeyDown());
		disguise.setPose(entity.getPose());
		disguise.setInvisible(entity.isInvisible());
		disguise.setSprinting(entity.isSprinting());
		disguise.setSwimming(entity.isSwimming());

		disguise.setCustomName(entity.getDisplayName());
		disguise.setCustomNameVisible(entity.isCustomNameVisible());
		disguise.setGlowingTag(entity.isCurrentlyGlowing());

		if (disguise instanceof LivingEntity livingDisguise) {
			livingDisguise.yBodyRot = entity.yBodyRot;
			livingDisguise.yBodyRotO = entity.yBodyRotO;

			livingDisguise.yHeadRot = entity.yHeadRot;
			livingDisguise.yHeadRotO = entity.yHeadRotO;

			PlayerDisguiseBehavior.copyWalkAnimation(entity.walkAnimation, livingDisguise.walkAnimation);

			livingDisguise.swingingArm = entity.swingingArm;
			livingDisguise.attackAnim = entity.attackAnim;
			livingDisguise.swingTime = entity.swingTime;
			livingDisguise.oAttackAnim = entity.oAttackAnim;
			livingDisguise.swinging = entity.swinging;

			livingDisguise.setOnGround(entity.onGround());

			livingDisguise.hurtTime = entity.hurtTime;
			livingDisguise.hurtDuration = entity.hurtDuration;
			livingDisguise.hurtMarked = entity.hurtMarked;

			for (final EquipmentSlot slot : EQUIPMENT_SLOTS) {
				final ItemStack stack = entity.getItemBySlot(slot);
				if (!stack.is(MinigameItems.DISGUISE.get())) {
					livingDisguise.setItemSlot(slot, stack);
				}
			}
		}

		disguise.tickCount = entity.tickCount;
	}

	// TODO: Shameless code duplication
	private static boolean shouldShowName(EntityRenderDispatcher entityRenderDispatcher, Player player) {
		if (ClientGameStateManager.getOrNull(GameClientStateTypes.HIDE_NAME_TAGS) != null) {
			return false;
		}

		double distanceSq = entityRenderDispatcher.distanceToSqr(player);
		float maximumDistance = player.isDiscrete() ? 32.0f : 64.0f;
		if (distanceSq >= maximumDistance * maximumDistance) {
			return false;
		}

		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer localPlayer = minecraft.player;
		boolean visible = !player.isInvisibleTo(localPlayer);
		if (player != localPlayer) {
			Team playerTeam = player.getTeam();
			Team localPlayerTeam = localPlayer.getTeam();
			if (playerTeam != null) {
				return switch (playerTeam.getNameTagVisibility()) {
					case ALWAYS -> visible;
					case NEVER -> false;
					case HIDE_FOR_OTHER_TEAMS ->
							localPlayerTeam == null ? visible : playerTeam.isAlliedTo(localPlayerTeam) && (playerTeam.canSeeFriendlyInvisibles() || visible);
					case HIDE_FOR_OWN_TEAM ->
							localPlayerTeam == null ? visible : !playerTeam.isAlliedTo(localPlayerTeam) && visible;
				};
			}
		}

		return Minecraft.renderNames() && player != minecraft.getCameraEntity() && visible && !player.isVehicle();
	}

	public static void updateClientDisguise(int id, DisguiseType disguiseType) {
		if (Minecraft.getInstance().level.getEntity(id) instanceof LivingEntity entity) {
			PlayerDisguise disguise = PlayerDisguise.getOrNull(entity);
			if (disguise != null) {
				disguise.set(disguiseType);
			}
		}
	}

	@SubscribeEvent
	public static void calculateCameraDistance(CalculateDetachedCameraDistanceEvent event) {
		if (event.getCamera().getEntity() instanceof Player player) {
			PlayerDisguise disguise = PlayerDisguise.getOrNull(player);
			if (disguise == null) {
				return;
			}
			float scale = Math.max(disguise.getEffectiveScale(), 1.0f);
			event.setDistance(event.getDistance() * scale);
		}
	}

	public static PlayerSkin getSkin(ResolvableProfile profile) {
		return SKIN_LOOKUP_CACHE.getUnchecked(profile).get();
	}

	private record DisguiseRenderState(
			@Nullable
			EntityRenderState entityRenderState,
			float scale
	) {
		public static DisguiseRenderState scaling(float scale) {
			return new DisguiseRenderState(null, scale);
		}
	}
}
