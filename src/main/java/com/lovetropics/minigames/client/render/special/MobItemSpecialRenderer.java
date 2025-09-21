package com.lovetropics.minigames.client.render.special;

import com.lovetropics.minigames.common.core.diguise.DisguiseType;
import com.lovetropics.minigames.common.core.item.MinigameDataComponents;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class MobItemSpecialRenderer implements SpecialModelRenderer<MobItemSpecialRenderer.Argument> {
	private final Minecraft minecraft;
	private final EntityRenderDispatcher entityRenderDispatcher;
	private final EntityInfoCache entityInfoCache;
	@Nullable
	private final ResourceLocation inventorySprite;

	private MobItemSpecialRenderer(Minecraft minecraft, EntitySource entitySource, @Nullable ResourceLocation inventorySprite) {
		this.minecraft = minecraft;
		entityRenderDispatcher = minecraft.getEntityRenderDispatcher();
		entityInfoCache = new EntityInfoCache(entityRenderDispatcher, entitySource);
		this.inventorySprite = inventorySprite;
	}

	private static void addVertex(final VertexConsumer consumer, final PoseStack.Pose pose, final float x, final float y, final float u, final float v, final int packedLight, int packedOverlay) {
		consumer.addVertex(pose, x, y, 0.0f)
				.setColor(1.0f, 1.0f, 1.0f, 1.0f)
				.setUv(u, v)
				.setOverlay(packedOverlay)
				.setLight(packedLight)
				.setNormal(pose, 0.0f, 1.0f, 0.0f);
	}

	private void drawInventorySprite(final PoseStack poseStack, final MultiBufferSource bufferSource, final int packedLight, final int packedOverlay) {
		if (inventorySprite == null) {
			return;
		}
		final ResourceLocation atlas = TextureAtlas.LOCATION_BLOCKS;
		final TextureAtlasSprite sprite = minecraft.getTextureAtlas(atlas).apply(inventorySprite);
		final VertexConsumer consumer = bufferSource.getBuffer(RenderType.textSeeThrough(atlas));
		final PoseStack.Pose pose = poseStack.last();
		addVertex(consumer, pose, 0.0f, 0.0f, sprite.getU0(), sprite.getV1(), packedLight, packedOverlay);
		addVertex(consumer, pose, 1.0f, 0.0f, sprite.getU1(), sprite.getV1(), packedLight, packedOverlay);
		addVertex(consumer, pose, 1.0f, 1.0f, sprite.getU1(), sprite.getV0(), packedLight, packedOverlay);
		addVertex(consumer, pose, 0.0f, 1.0f, sprite.getU0(), sprite.getV0(), packedLight, packedOverlay);
	}

	@Override
	public void render(@Nullable Argument argument, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, boolean hasFoilType) {
		drawEntity(argument, displayContext, poseStack, bufferSource, packedLight);
		if (displayContext == ItemDisplayContext.GUI) {
			drawInventorySprite(poseStack, bufferSource, packedLight, packedOverlay);
		}
	}

	private void drawEntity(@Nullable Argument argument, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		if (argument == null) {
			return;
		}

		poseStack.pushPose();
		poseStack.translate(0.5f, 0.5f, 0.5f);
		applyTransforms(argument, displayContext, poseStack);

		entityRenderDispatcher.render(argument.entityInfo.renderState(), 0.0, 0.0, 0.0, poseStack, bufferSource, packedLight);

		poseStack.popPose();
	}

	private void applyTransforms(final Argument argument, final ItemDisplayContext context, final PoseStack poseStack) {
		final float scale = getScale(argument, context);
		poseStack.scale(scale, scale, scale);

		final boolean left = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
		switch (context) {
			case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
				poseStack.mulPose(Axis.YP.rotationDegrees(left ? 25.0f : -25.0f));
				poseStack.translate(0.0f, -0.2f / scale, 0.0f);
			}
			case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
				poseStack.mulPose(Axis.YP.rotationDegrees(left ? 45.0f : -45.0f));
				poseStack.translate(0.0f, -0.1f / scale, -argument.entityInfo.width() / 2.0f);
			}
			case HEAD -> {
				poseStack.translate(0.0f, 0.375f / scale, 0.0f);
				poseStack.mulPose(Axis.YP.rotation(Mth.PI));
			}
			case GUI -> {
				poseStack.translate(0.0f, -argument.entityInfo.height() / 2.0f, 0.0f);
				poseStack.mulPose(Axis.XP.rotationDegrees(25.0f));
				poseStack.mulPose(Axis.YP.rotationDegrees(315.0f));
			}
			case GROUND -> poseStack.translate(0.0f, -0.2f / scale, 0.0f);
			case FIXED -> {
				poseStack.translate(0.0f, -0.2f / scale - argument.entityInfo.height() / 2.0f, 0.0f);
				poseStack.mulPose(Axis.YP.rotation(Mth.PI));
			}
		}
	}

	private float getScale(final Argument argument, final ItemDisplayContext context) {
		final float targetSize = argument.targetSize() * switch (context) {
			case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> 0.8f;
			case HEAD -> 1.5f;
			case GUI, FIXED -> 0.9f;
			case GROUND -> 0.5f;
			default -> 1.0f;
		};
		return targetSize / Math.max(argument.entityInfo.approximateSize(), 1.0f);
	}

	@Override
	public void getExtents(Set<Vector3f> output) {
		output.add(new Vector3f(0.0f, 0.0f, 0.0f));
		output.add(new Vector3f(1.0f, 0.0f, 0.0f));
		output.add(new Vector3f(0.0f, 1.0f, 0.0f));
		output.add(new Vector3f(1.0f, 1.0f, 0.0f));
		output.add(new Vector3f(0.0f, 0.0f, 1.0f));
		output.add(new Vector3f(1.0f, 0.0f, 1.0f));
		output.add(new Vector3f(0.0f, 1.0f, 1.0f));
		output.add(new Vector3f(1.0f, 1.0f, 1.0f));
	}

	@Override
	@Nullable
	public Argument extractArgument(ItemStack stack) {
		ClientLevel level = minecraft.level;
		if (level == null) {
			return null;
		}
		EntityInfo info = entityInfoCache.get(level, stack);
		if (info == null) {
			return null;
		}
		float targetSize = stack.getOrDefault(MinigameDataComponents.SIZE, 1.0f);
		return new Argument(info, targetSize);
	}

	private static class EntityInfoCache {
		private final EntityRenderDispatcher entityRenderDispatcher;
		private final EntitySource entitySource;

		@Nullable
		private WeakReference<ClientLevel> level;
		private final Map<DisguiseType.EntityConfig, Optional<EntityInfo>> entities = new Object2ObjectOpenHashMap<>();

		private EntityInfoCache(EntityRenderDispatcher entityRenderDispatcher, EntitySource entitySource) {
			this.entityRenderDispatcher = entityRenderDispatcher;
			this.entitySource = entitySource;
		}

		@Nullable
		public EntityInfo get(final ClientLevel level, final ItemStack itemStack) {
			DisguiseType.EntityConfig type = entitySource.get(itemStack);
			if (type == null) {
				return null;
			}
			if (this.level == null || this.level.get() != level) {
				entities.clear();
				this.level = new WeakReference<>(level);
			}
			Optional<EntityInfo> info = entities.get(type);
			if (info == null) {
				info = extractInfo(type.createEntity(level));
				entities.put(type, info);
			}
			return info.orElse(null);
		}

		private <T extends Entity> Optional<EntityInfo> extractInfo(@Nullable T entity) {
			if (entity == null) {
				return Optional.empty();
			}
			EntityRenderer<? super T, ?> renderer = entityRenderDispatcher.getRenderer(entity);
			return Optional.of(new EntityInfo(
					renderer.createRenderState(entity, 1.0f),
					entity.getBbWidth(),
					entity.getBbHeight(),
					// Approximate size of the entity - overestimate width a bit because bounding boxes are usually too small
					Math.max(entity.getBbWidth() * 2.0f, entity.getBbHeight())
			));
		}
	}

	public record EntityInfo(
			EntityRenderState renderState,
			float width,
			float height,
			float approximateSize
	) {
	}

	public record Argument(
			EntityInfo entityInfo,
			float targetSize
	) {
	}

	public record Unbaked(
			EntitySource entitySource,
			Optional<ResourceLocation> inventorySprite
	) implements SpecialModelRenderer.Unbaked {
		public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				EntitySource.CODEC.fieldOf("entity_source").forGetter(Unbaked::entitySource),
				ResourceLocation.CODEC.optionalFieldOf("inventory_sprite").forGetter(Unbaked::inventorySprite)
		).apply(i, Unbaked::new));

		@Override
		public MapCodec<Unbaked> type() {
			return MAP_CODEC;
		}

		@Override
		public SpecialModelRenderer<?> bake(EntityModelSet modelSet) {
			return new MobItemSpecialRenderer(Minecraft.getInstance(), entitySource, inventorySprite.orElse(null));
		}
	}

	public enum EntitySource implements StringRepresentable {
		DISGUISE("disguise", stack -> {
			DisguiseType disguise = stack.get(MinigameDataComponents.DISGUISE);
			return disguise != null ? disguise.entity() : null;
		}),
		ENTITY("entity", stack -> stack.get(MinigameDataComponents.ENTITY)),
		;

		public static final Codec<EntitySource> CODEC = StringRepresentable.fromEnum(EntitySource::values);

		private final String name;
		private final Function<ItemStack, DisguiseType.EntityConfig> extractor;

		EntitySource(String name, Function<ItemStack, DisguiseType.EntityConfig> extractor) {
			this.name = name;
			this.extractor = extractor;
		}

		@Nullable
		public DisguiseType.EntityConfig get(ItemStack stack) {
			return extractor.apply(stack);
		}

		@Override
		public String getSerializedName() {
			return name;
		}
	}
}
