package com.lovetropics.minigames.common.content.paint_party.entity;

import com.lovetropics.minigames.common.content.paint_party.PaintParty;
import com.lovetropics.minigames.common.content.paint_party.PaintPartyEvents;
import com.lovetropics.minigames.common.core.game.IGamePhase;
import com.lovetropics.minigames.common.core.game.impl.GamePhaseManager;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

public class PaintBallEntity extends ThrowableProjectile implements ItemSupplier {
	private static final ItemStack DEFAULT_VISUAL_ITEM = new ItemStack(Items.WHITE_CONCRETE);

	private static final EntityDataAccessor<ItemStack> VISUAL_ITEM = SynchedEntityData.defineId(PaintBallEntity.class, EntityDataSerializers.ITEM_STACK);

	public PaintBallEntity(EntityType<? extends ThrowableProjectile> entityType, Level level) {
		super(entityType, level);
	}

	public PaintBallEntity(Level level) {
		this(PaintParty.PAINTBALL.get(), level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(VISUAL_ITEM, DEFAULT_VISUAL_ITEM);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.store("visual_item", ItemStack.CODEC, getItem());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setVisualItem(input.read("visual_item", ItemStack.CODEC).orElse(DEFAULT_VISUAL_ITEM));
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		super.onHitBlock(result);

		IGamePhase game = GamePhaseManager.get().getGamePhaseAt(level(), result.getBlockPos());
		if (game != null) {
			game.invoker(PaintPartyEvents.PAINTBALL_HIT).onPaintBallHit(level(), this, result.getBlockPos());
		}

		discard();
	}

	@Override
	public void shootFromRotation(Entity shooter, float x, float y, float z, float velocity, float inaccuracy) {
		shoot(
				-Mth.sin(y * Mth.DEG_TO_RAD) * Mth.cos(x * Mth.DEG_TO_RAD),
				-Mth.sin((x + z) * Mth.DEG_TO_RAD),
				Mth.cos(y * Mth.DEG_TO_RAD) * Mth.cos(x * Mth.DEG_TO_RAD),
				velocity,
				inaccuracy
		);
	}

	public void setVisualItem(ItemStack itemStack) {
		getEntityData().set(VISUAL_ITEM, itemStack);
	}

	@Override
	public ItemStack getItem() {
		return getEntityData().get(VISUAL_ITEM);
	}
}
